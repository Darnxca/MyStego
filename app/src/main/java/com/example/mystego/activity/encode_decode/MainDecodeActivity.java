package com.example.mystego.activity.encode_decode;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mystego.moduli.progetto.modulo_compressione.LZW;
import com.example.mystego.moduli.progetto.modulo_hashing.Hashing;
import com.example.mystego.moduli.progetto.modulo_steganografico.BitmapEncoder;
import com.example.mystego.R;
import com.example.mystego.moduli.progetto.modulo_crittografico.RSA_Decode;
import com.example.mystego.utils.Utils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MainDecodeActivity extends AppCompatActivity {

    private Button btn_decode;
    private TextView textArea;
    private ImageView imageView;
    private Bitmap original_image;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_decode);

        btn_decode = findViewById(R.id.decifra);
        Button btn_choseImg = findViewById(R.id.chose_img2);

        imageView = findViewById(R.id.imageview1);
        textArea = findViewById(R.id.textArea);

        btn_decode.setEnabled(false);

        btn_choseImg.setOnClickListener(view -> ImageChooser());

        btn_decode.setOnClickListener(view -> {

            try {
                // estrae il messaggio dall'immagine
                byte[] decodedBitmap = BitmapEncoder.decode(original_image);

                // istanzia un decodificatore RSA
                RSA_Decode rsa = new RSA_Decode(getApplicationContext());

                // decripta il messaggio
                String decryptedMessage = rsa.decrypt(decodedBitmap);

                //Trasforma la stringa decriptata in una lista di interi perchè LZW utilizza interi
                List<Integer> messageDecrypted = Utils.stringToList(decryptedMessage);

                // decomprimo il messaggio
                String testoDecompresso = LZW.decode(messageDecrypted);

                // controllo se il testo decompresso è stato compromesso
                checkMessage(testoDecompresso);
            } catch (NegativeArraySizeException e){
                textArea.setText("Testo compromesso");
            }
        });

        //activity on result del metodo chooser, usato per mostrare l'immagine scelta
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        Uri filepath = data.getData();


                        try {
                            original_image = MediaStore.Images.Media.getBitmap(getContentResolver(), filepath);
                            imageView.setImageBitmap(original_image);
                            btn_decode.setEnabled(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
    }

    /**
     *  chooser per scegliere un immagine
     */
    private void ImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        activityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    /**
     *
     * @param testoDecompresso testo decompresso ottenuto dall'immagine
     *
     *  Il metodo confronta il valore hash di testoDecompresso con quello memorizzato su firebase
     */
    private void checkMessage(String testoDecompresso) {
        String h = Hashing.md5(testoDecompresso);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("HASH/"+h);

        ValueEventListener postListener = new ValueEventListener() {

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    Objects.requireNonNull(snapshot.child("hash").getValue(String.class));
                    textArea.setText(testoDecompresso);

                }catch (NullPointerException e){
                    textArea.setText("Testo compromesso");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        reference.addValueEventListener(postListener);
    }
}