package com.example.mystego.moduli.progetto.modulo_crittografico;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;

public class RSA_Encode {

	private static final String PRIVATE_KEY_FILE = "Private.key";

	@RequiresApi(api = Build.VERSION_CODES.O)
	public RSA_Encode(){ }

	/**
	 *
	 * @param context contesto dell'app
	 * @param privateKey chiave privata
	 *
	 * Metodo utilizzato per salvare i parametri della chiave privata sul dispositivo
	 */
	public static void saveKeyParameter(Context context, PrivateKey privateKey) {
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance("RSA");

			RSAPrivateKeySpec rsaPrivKeySpec = keyFactory.getKeySpec(privateKey, RSAPrivateKeySpec.class);

			saveKeys(context, rsaPrivKeySpec.getModulus(), rsaPrivKeySpec.getPrivateExponent());
		} catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	/**
	 * @params message messaggio da crittografare
	 * @porams pubKey chiave pubblica di un utente
	 *
	 * Metodo che effettua la crittografia tramite RSA di un messaggio
	 */
	public byte[] encrypt(String message, PublicKey pubKey){

		byte[] messageToBytes = message.getBytes();
		byte[] encryptedBytes = null;
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE,pubKey);
			encryptedBytes = cipher.doFinal(messageToBytes);

			//insertMessageInImage(encryptedBytes);
		} catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return encryptedBytes;
	}

	/**
	 *
	 * @param context contesto dell'app
	 * @param mod modulo della chiave privata
	 * @param exp esponente della chiave privata
	 * @throws IOException
	 *
	 * Metodo per salvare la chiave privata sul dispositivo
	 */
	private static void saveKeys(Context context, BigInteger mod, BigInteger exp) throws IOException{

		File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + RSA_Encode.PRIVATE_KEY_FILE);

		FileOutputStream fos = null;
		ObjectOutputStream oos = null;

		try {
			System.out.println("Generating "+ RSA_Encode.PRIVATE_KEY_FILE + "...");
			fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(new BufferedOutputStream(fos));

			oos.writeObject(mod);
			oos.writeObject(exp);

			System.out.println(RSA_Encode.PRIVATE_KEY_FILE + " generated successfully");
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(oos != null){
				oos.close();
				fos.close();
			}
		}
	}

}