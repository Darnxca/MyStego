package com.example.mystego.moduli.progetto.modulo_crittografico;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class RSA_Decode {

	private static final String PRIVATE_KEY_FILE = "Private.key";
	private final Context context;
	
	public RSA_Decode(Context context){this.context = context;}

	/**
	 *
	 * @param encrypt dati criptati sotto forma di byte
	 * @return striga criptata
	 *
	 * Metodo che decripta una stringa tramite l'uso RSA
	 */
	public String decrypt(byte[] encrypt){
		byte[] decryptedMessage = null;

		// ottengo la chiave privata dal file salvato sul dispositivo
        try {
	        PrivateKey privateKey = readPrivateKeyFromFile(PRIVATE_KEY_FILE);
	        Cipher cipher = Cipher.getInstance("RSA");
	        cipher.init(Cipher.DECRYPT_MODE,privateKey);
	        decryptedMessage = cipher.doFinal(encrypt);
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return new String(decryptedMessage);
    }

	/**
	 *
	 * @param fileName filename
	 * @return privateKey chiava private dell'utente
	 * @throws IOException
	 *
	 * Metodo per leggere la chive privata memorizzata sul dispositivo
	 */
    public PrivateKey readPrivateKeyFromFile(String fileName) throws IOException{

		File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" +fileName);
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			
			BigInteger modulus = (BigInteger) ois.readObject();
		    BigInteger exponent = (BigInteger) ois.readObject();
			
		    //Get Private Key
		    RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(modulus, exponent);
		    KeyFactory fact = KeyFactory.getInstance("RSA");

			return fact.generatePrivate(rsaPrivateKeySpec);
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(ois != null){
				ois.close();
				fis.close();
			}
		}
		return null;
	}
}
