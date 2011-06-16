package com.android.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.android.service.auto.Cfg;
import com.android.service.util.Check;
import com.android.service.util.Utils;

public class Messages {
	private static final String TAG = "Messages"; //$NON-NLS-1$

	private static HashMap<String, String> messages;
	private static boolean initialized;

	private Messages() {
	}

	public synchronized static boolean init(Context context) {
		if (initialized) {
			return true;
		}
		
		if (Cfg.DEBUG) { Check.asserts(context!=null, " (init) Assert failed"); }

		try {
			messages = new HashMap<String, String>();
			Resources resources = context.getResources();
			InputStream stream = resources.openRawResource(R.raw.messages);

			
			SecretKey key = produceKey("123456");
			if (Cfg.DEBUG) {
				Check.asserts(key != null, "null key"); //$NON-NLS-1$
			}

			if (Cfg.DEBUG) {
				Check.log(TAG + " (init): stream=" + stream.available());
				Check.log(TAG + " (init): key=" + Utils.byteArrayToHex(key.getEncoded()));
			}
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); //$NON-NLS-1$
			final byte[] iv = new byte[16];
			Arrays.fill(iv, (byte) 0);
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
			CipherInputStream cis = new CipherInputStream(stream, cipher);

			BufferedReader br = new BufferedReader(new InputStreamReader(cis));

			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}

				String[] kv = line.split("="); //$NON-NLS-1$
				if (Cfg.DEBUG) {
					Check.asserts(kv.length == 2, "wrong number of tokens"); //$NON-NLS-1$
					Check.log(TAG + " " + kv[0] + " " + kv[1]); //$NON-NLS-1$ //$NON-NLS-2$
				}

				messages.put(kv[0], kv[1]);

				if (Cfg.DEBUG) {
					Check.asserts(messages.containsKey(kv[0]), "strange hashmap behaviour"); //$NON-NLS-1$
				}
			}

			initialized = true;
		} catch (Exception ex) {
			return false;
		}
		return true;

	}

	public static String getString(String key) {
		if (!initialized) {
			init(Status.getAppContext());
		}
		try {
			if (Cfg.DEBUG) {
				Check.asserts(messages.containsKey(key), "no key known: " + key); //$NON-NLS-1$
			}
			String str = messages.get(key);
			return str;
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/**
	 * Reads the contents of the key file and converts this into a
	 * <code>Key</code>.
	 * 
	 * @return The <code>Key</code> object.
	 * @throws BuildException
	 *             If the contents of the key file cannot be read.
	 */
	private static SecretKey produceKey(String key) {

		try {
			if (Cfg.DEBUG) {
				Check.log(" key: " + key + " " + key.length()); //$NON-NLS-1$
			}
			String salt="sale";
			
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			
			for(int i=0; i<128;i++){
				digest.update(salt.getBytes());
				digest.update(key.getBytes());
				digest.update(digest.digest());
			}
			
			byte[] sha1 = digest.digest();
			
			byte[] aes_key = new byte[16];
			System.arraycopy(sha1, 0, aes_key, 0, aes_key.length);
									
			SecretKey secret = new SecretKeySpec(aes_key, "AES");
			return secret;

		} catch (Exception e) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " " + e); //$NON-NLS-1$
			}
			return null;
		}

	}

}
