package com.example.mystego.activity.encode_decode;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.mystego.Adapter.SpinnerAdapter;
import com.example.mystego.moduli.progetto.modulo_compressione.LZW;
import com.example.mystego.moduli.progetto.modulo_hashing.Hashing;
import com.example.mystego.moduli.progetto.modulo_steganografico.BitmapEncoder;
import com.example.mystego.R;
import com.example.mystego.firebase.model.User.User;
import com.example.mystego.moduli.progetto.modulo_crittografico.RSA_Encode;
import com.example.mystego.utils.AndroidUtils;
import com.example.mystego.utils.Utils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainEncodeActivity extends AppCompatActivity {

    private EditText editText;
    private Button btn_encode, btn_choseImage;
    private ImageView imageView;
    private List<Integer> testoCompresso = new ArrayList<>();
    private Bitmap original_image;
    private PublicKey publicKey;
    private RSA_Encode rsa;
    private SpinnerAdapter spinnerAdapter;
    private String userId, imgName;
    private File file;
    private ActivityResultLauncher<Intent> activityResultChoseImg, activityResultEmail;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_encode);

        imageView = findViewById(R.id.imageview);
        editText = findViewById(R.id.editText1);
        btn_encode = findViewById(R.id.Cifra);
        btn_choseImage = findViewById(R.id.chose_img);
        Spinner spinner = findViewById(R.id.Spinner);

        //Disabilito i bottoni ecnode e editText
        btn_encode.setEnabled(false);
        editText.setEnabled(false);

        // richiedo i permessi per salvare l'immagine e il file della chiave privata sul dispositivo dell'utente
        AndroidUtils.checkAndRequestPermissions(getApplicationContext(),this);

        //Creo una select con tutti gli utenti registrati su firebase
        getUsers(spinner);

        // metodo usato per ottenere l'utente selezionato
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                userId = spinnerAdapter.getItem(i).getIdUsr();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // metodo usato per controllare che la size in byte del messaggio non sia superiore a 245 byte (limite RSA 2048)
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int end) {
                testoCompresso = LZW.encode(charSequence.toString());
                if (Utils.itsByteSizeCorrect(testoCompresso)) {
                    Toast.makeText(getApplicationContext(), "Testo troppo grande per essere inserito nell'immagine",
                            Toast.LENGTH_SHORT).show();
                    btn_encode.setEnabled(false);
                    btn_choseImage.setEnabled(false);
                } else {
                    btn_choseImage.setEnabled(true);
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int end) {
                testoCompresso = LZW.encode(charSequence.toString());
                if (Utils.itsByteSizeCorrect(testoCompresso)) {
                    Toast.makeText(getApplicationContext(), "Testo troppo grande per essere inserito nell'immagine",
                            Toast.LENGTH_SHORT).show();
                    btn_encode.setEnabled(false);
                } else {
                    btn_encode.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        btn_choseImage.setOnClickListener(view -> ImageChooser());

        btn_encode.setOnClickListener(view -> {

            //instanza di RSA
            rsa = new RSA_Encode();

            encodeMessageWithUserIdPublicKey(userId);
        });

        //activity on result del metodo chooser, usato per mostrare l'immagine scelta
        activityResultChoseImg = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        Uri filepath = data.getData();
                        try {
                            original_image = MediaStore.Images.Media.getBitmap(getContentResolver(), filepath);
                            imageView.setImageBitmap(original_image);

                            String[] stringData = (new File(String.valueOf(filepath))).getName().split("%2F");
                            imgName = stringData[stringData.length - 1];

                            editText.setEnabled(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

        //activity onresult utilizzato per cancellare l'immagine dopo che è stata inviata via email
        activityResultEmail = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {

                        // calcolo l'hash del messaggio e lo salvo su firebase
                        saveHash(Hashing.md5(editText.getText().toString()));

                        // cancello l'immagine dal dispositivo
                        deleteTmpImg(file);

                        // invio una notifica di conferma invio
                        AndroidUtils.sendNotification("Immagine inviata via email!",getApplicationContext());

                        finish();
                    }
                });
    }

    /**
     *
     * @param spinner
     *
     * crea una select con tutti gli utenti di firebase
     */
    private void getUsers(Spinner spinner) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ArrayList<User> usr = new ArrayList<>();
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String idUsr = ds.getKey();
                    String name = ds.child("name").getValue(String.class);
                    String email = ds.child("email").getValue(String.class);
                    usr.add(new User(idUsr, name, email));
                }
                spinnerAdapter = new SpinnerAdapter(getApplicationContext(), android.R.layout.simple_spinner_item,
                        usr);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        ref.addListenerForSingleValueEvent(eventListener);
    }

    /**
     *
     * @param userId : id dell'utente a cui si desidera inviare l'immagine
     *
     * metodo che effettua la codifica RSA e incapsula il testo cifrato in un'immagine
     */
    public void encodeMessageWithUserIdPublicKey(String userId) {

        // prendo il riferimento su firebase dell'utente a cui voglio inviare
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users/" + userId);

        ValueEventListener postListener = new ValueEventListener() {

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // prendo il modulo e l'espondente della chiave pubblica dell'utente a cui inviare
                BigInteger modulus = new BigInteger(Objects.requireNonNull(snapshot.child("modulo").getValue(String.class)));
                BigInteger exponent = new BigInteger(Objects.requireNonNull(snapshot.child("esponente").getValue(String.class)));

                // mi genero la chiave pubblica tramite modulo e esponente
                RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
                KeyFactory fact;

                // riferimento all'email dell'utente su firebase
                String emailU = Objects.requireNonNull(snapshot.child("email").getValue(String.class));
                try {
                    fact = KeyFactory.getInstance("RSA");
                    publicKey = fact.generatePublic(rsaPublicKeySpec);

                    // cifro il testo compresso con la chiave pubblica dell'utente a cui inviare il messaggio
                    byte[] encryptedMessage = rsa.encrypt(Utils.listToString(testoCompresso), publicKey);

                    // incapsulo il messaggio nell'immagine
                    Bitmap encodedBitmap = BitmapEncoder.encode(original_image, encryptedMessage);

                    // salvo l'immmagine temporaneamente
                    file = saveTemporyImg(encodedBitmap, imgName);

                    // invio l'email con l'immagine
                    sendEmail(file,emailU);

                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        reference.addValueEventListener(postListener);
    }

    /**
     *  chooser per scegliere un immagine
     */
    private void ImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activityResultChoseImg.launch(Intent.createChooser(intent, "Select Picture"));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    /**
     * @params bitmapImage immagine sotto formato di bitmap
     * @params imgName nome dell'immagine che si vuole inviare
     *
     * salva l'immagine temporaneamente sul dispositivo dell'utente
     */
    private File saveTemporyImg(Bitmap bitmapImage, String imgName) {
        OutputStream fOut;

        // calcolo di un timestamp
        long tsLong = System.currentTimeMillis()/1000;
        String timeStamp = Long.toString(tsLong);

        // costruisco il nome dell'immagine come timestamp+ nome immagine + encoded + png
        String newImgName = timeStamp+ imgName.split("\\.")[0] +"encoded" + ".PNG";

        //  Creo un file nella cartella dowload
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), newImgName);

        // Viene inserita l'immagine nel file
        try {
            fOut = new FileOutputStream(file);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     *
     * @param file file da inviare
     * @param email a cui inviare
     *
     * Invio un email all'utente con allegato l'immagine sotto formato di file
     */
    private void sendEmail(File file, String email) {
        Uri path = FileProvider.getUriForFile(getApplicationContext(), this.getApplicationContext().getPackageName() + ".provider", file);

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // setto l'intent come email
        emailIntent .setType("application/image");
        String[] to = {email};
        emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
        // aggiungo il gile
        emailIntent .putExtra(Intent.EXTRA_STREAM, path);
        // aggiungo il soggetto all'email
        emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Subject");
        activityResultEmail.launch(Intent.createChooser(emailIntent, "end email..."));
    }

    /**
     *
     * @param hash valore hash del testo
     *
     *  Salvo il valore hash su firebase
     */
    private void saveHash(String hash){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("HASH");
        reference.child(hash).child("hash").setValue(hash);
    }

    /**
     *
     * @param file
     *
     * cancello il file dal dispositivo
     */
    private void deleteTmpImg(File file){
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("file Deleted :");
            } else {
                System.out.println("file not Deleted :");
            }
        }
    }
}