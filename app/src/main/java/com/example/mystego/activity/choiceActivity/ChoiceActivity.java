package com.example.mystego.activity.choiceActivity;

import static com.example.mystego.moduli.progetto.modulo_crittografico.RSA_Encode.saveKeyParameter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.example.mystego.activity.main.MainActivity;
import com.example.mystego.R;
import com.example.mystego.activity.encode_decode.MainDecodeActivity;
import com.example.mystego.activity.encode_decode.MainEncodeActivity;
import com.example.mystego.firebase.model.User.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

public class ChoiceActivity extends Activity {

    private Button btn_encode, btn_decode;
    private Context context;

    private DatabaseReference reference;
    private String id, name, email;

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        btn_encode = findViewById(R.id.Encode);
        btn_decode = findViewById(R.id.Decode);
        Button btn_logout = findViewById(R.id.Logout);
        Button btn_keygen = findViewById(R.id.keygen);
        TextView txNome = findViewById(R.id.txNome);

        //Controlla se è già stato effettuato un accesso con google altrimenti richiede di accedere
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (signInAccount != null) {

            // prendo i parametri che mi interessano dall'account di google
            id = signInAccount.getId();
            name = signInAccount.getDisplayName();
            email = signInAccount.getEmail();

            txNome.setText("Welcome " + name);

            // creo un riferimento al database di firebase
            reference = FirebaseDatabase.getInstance().getReference();

            // Creo un nuovo utente su firebase solo se non esite
            reference = FirebaseDatabase.getInstance().getReference("Users");
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.hasChild(id)) {
                        writeNewUser(id, name, email);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


            btn_encode.setOnClickListener(view -> startActivity(new Intent(context, MainEncodeActivity.class).putExtra("user", id)));

            btn_decode.setOnClickListener(view -> startActivity(new Intent(context, MainDecodeActivity.class)));

            // bottone che permette la generazione di nuove chiavi private e pubbliche
            btn_keygen.setOnClickListener(view -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChoiceActivity.this);
                builder.setTitle(R.string.dialog_title)
                        .setMessage(R.string.dialog_message)
                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                            generatekey(id);
                            btn_encode.setEnabled(true);
                            btn_decode.setEnabled(true);
                        })
                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create();
                builder.show();
            });


            btn_logout.setOnClickListener(view -> {
                FirebaseAuth.getInstance().signOut();
                GoogleSignInOptions gso = new GoogleSignInOptions.
                        Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).
                        build();

                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
                googleSignInClient.signOut();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            });

            if (!keyExists()) {
                btn_encode.setEnabled(false);
                btn_decode.setEnabled(false);
            }
        }


    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    /**
     * Metodo per controllare se è già stata creata in precedenza una chiave privata controllando
     * se è stato salvato in una directory
     */
    private boolean keyExists() {
        final String PRIVATE_KEY_FILE = "Private.key";
        Path path = Paths.get(String.valueOf(new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + PRIVATE_KEY_FILE)));
        return Files.exists(path);
    }

    /**
     *
     * @param userId id dell'utente
     * @param name nome dell'utente
     * @param email email dell'utente
     *
     *  Metodo per registrare un nuovo utente su firebase
     */
    private void writeNewUser(String userId, String name, String email) {
        User user = new User(name, email);
        reference.child(userId).setValue(user);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    /**
     * @param userId id dell'utente
     *
     * metodo per generare e salvare le chiavi di un utente
     */
    private void generatekey(String userId) {
        KeyPairGenerator generator;

        PublicKey publicKey;
        PrivateKey privateKey;

        try {
            // instanzia un generatore basato su RSA
            generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            //genera le chiavi pubbliche e privata
            KeyPair pair = generator.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();

            KeyFactory keyFactory;

            //Restituisce un oggetto KeyFactory che converte le chiavi pubbliche/private dell'algoritmo specificato.
            keyFactory = KeyFactory.getInstance("RSA");

            RSAPublicKeySpec rsaPubKeySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);

            // otteniamo il modulo e l'esponente della chiave pubblica
            BigInteger mod = rsaPubKeySpec.getModulus();
            BigInteger exp = rsaPubKeySpec.getPublicExponent();

            // salvo su firebase modulo e esponente della chiave pubblica dell'utente
            reference.child(userId).child("modulo").setValue(mod + "");
            reference.child(userId).child("esponente").setValue(exp + "");

            // metodo utilizzato per salvare la chiave privata sul proprio dispositivo
            saveKeyParameter(context, privateKey);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

}