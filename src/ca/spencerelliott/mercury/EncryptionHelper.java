package ca.spencerelliott.mercury;

/************************************************************************
 * This file is part of Mercury.
 *
 * Mercury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mercury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Mercury.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Spencer Elliott
 * @author spencer@spencerelliott.ca
 ************************************************************************/

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import android.util.Log;

public class EncryptionHelper {
	//Log tag
	private final static String LOG_TAG = "Mercury.EncryptionHelper";
	
	//Constants for encryption methods
	private final int SALT_LOOP = 8;
	private final String PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
	private final String RAND_ALGORITHM = "SHA1PRNG";
	
	//Ciphers and the stored salt
	private Cipher encryption_cipher = null;
	private Cipher decryption_cipher = null;
	private byte[] salt = null;
	
	//The secret key for the encryption
	private SecretKey set_key = null;
	
	//Just initializes the object with the private key and generates a random salt
	private EncryptionHelper(char[] private_key) {
		//Creates a new salt with 16 bytes
		byte[] salt = new byte[16];
		
		//This will create the random salt
		SecureRandom rand;
		
		try {
			//Get the instance of the random salt generator
			rand = SecureRandom.getInstance(RAND_ALGORITHM);
			
			//Get the new salt
			rand.nextBytes(salt);
		} catch (NoSuchAlgorithmException e) {
			Log.e(EncryptionHelper.LOG_TAG, e.getMessage());
		}
		
		//Store the salt in the object
		this.salt = salt;
		
		//Use the new salt to initialize
		init(private_key, salt);
	}
	
	//Just initializes the object with the private key and salt
	private EncryptionHelper(char[] private_key, byte[] salt) {
		init(private_key, salt);
	}
	
	//Creates new instances of the encryption helper for use in other parts of the application
	public static EncryptionHelper getInstance(char[] private_key, byte[] salt) {
		return new EncryptionHelper(private_key, salt);
	}
	
	public static EncryptionHelper getInstance(char[] private_key) {
		return new EncryptionHelper(private_key);
	}
	
	//Initializes the object with the private key and salt
	public boolean init(char[] private_key, byte[] salt) {
		//Creates a key spec based on the private key
		PBEKeySpec key_spec = new PBEKeySpec(private_key);
		
		try {
			//Uses the secret key factory with the PBE algorithm and generates a secret based on the private key
			SecretKeyFactory key_factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
			set_key = key_factory.generateSecret(key_spec);
			
			//Creates PBE parameters using the salt and the iterations it needs to do
			PBEParameterSpec pbe_params = new PBEParameterSpec(salt, SALT_LOOP);
			
			//Creates instances of the encryption and decryption ciphers
			encryption_cipher = Cipher.getInstance(PBE_ALGORITHM);
			decryption_cipher = Cipher.getInstance(PBE_ALGORITHM);
			
			//Initialize the ciphers with the key and parameters
			encryption_cipher.init(Cipher.ENCRYPT_MODE, set_key, pbe_params);
			decryption_cipher.init(Cipher.DECRYPT_MODE, set_key, pbe_params);
		} catch(Exception e) {
			Log.e(EncryptionHelper.LOG_TAG, e.getMessage());
			return false;
		}
		return true;
	}
	
	public String decrypt(byte[] text) {
		//Create a new byte array to store the decrypted text
		byte[] decrypted_text = null;
		
		try {
			text = Base64.decode(text);
		} catch (IOException e) {
			
		}
		
		try {
			//Try to decode the text
			decrypted_text = decryption_cipher.doFinal(text);
		} catch (Exception e) {
			Log.e(EncryptionHelper.LOG_TAG, e.getMessage());
			return null;
		}
		
		//Return the decrypted string
		return new String(decrypted_text);
	}
	
	public byte[] encrypt(String text) {
		//Create a new byte array to store the encrypted text
		byte[] encrypted_text = null;
		
		try {
			//Try to encrypt the text
			encrypted_text = encryption_cipher.doFinal(text.getBytes());
		} catch(Exception e) {
			Log.e(EncryptionHelper.LOG_TAG, e.getMessage());
			return null;
		}
		
		encrypted_text = Base64.encodeBytesToBytes(encrypted_text);
		
		//Return the encrypted text
		return encrypted_text;
	}
	
	//Returns the salt in this object
	public byte[] getSalt() {
		return salt;
	}
}
