package com.ht.RCSAndroidGUI.crypto;

import java.util.Arrays;

import com.ht.RCSAndroidGUI.Debug;
import com.ht.RCSAndroidGUI.utils.Check;
import com.ht.RCSAndroidGUI.utils.Utils;

public class EncryptionPKCS5 extends Encryption {
    public EncryptionPKCS5(byte[] key) {
		super(key);
	}

	public EncryptionPKCS5() {
		super(Keys.self().getAesKey());
	}

	private static final int DIGEST_LENGTH = 20;
	//#ifdef DEBUG
    private static Debug debug = new Debug("EncryptionPKCS5"
            );
    //#endif
    /**
     * Gets the next multiple.
     * 
     * @param len
     *            the len
     * @return the next multiple
     */
    @Override
    public int getNextMultiple(final int len) {
        //#ifdef DBC
        Check.requires(len >= 0, "len < 0");
        //#endif

        final int newlen = len + (16 - len % 16);

        //#ifdef DBC
        Check.ensures(newlen > len, "newlen <= len");
        //#endif
        //#ifdef DBC
        Check.ensures(newlen % 16 == 0, "Wrong newlen");
        //#endif
        return newlen;
    }

    @Override
    protected byte[] pad(byte[] plain, int offset, int len) {
        return pad(plain, offset, len, true);
    }

    @Override
    public byte[] decryptData(final byte[] cyphered, final int enclen,
            final int offset) throws CryptoException {
        //#ifdef DEBUG
        debug.trace("decryptData PKCS5");
        //#endif

        //int padlen = cyphered[cyphered.length -1];
        //int plainlen = enclen - padlen;

        //#ifdef DBC
        Check.requires(enclen % 16 == 0, "Wrong padding");
        //Check.requires(enclen >= plainlen, "Wrong plainlen");
        //#endif

        final byte[] paddedplain = new byte[enclen];
        byte[] plain = null;
        int plainlen = 0;
        byte[] iv = new byte[16];

        final byte[] pt = new byte[16];

        final int numblock = enclen / 16;
        for (int i = 0; i < numblock; i++) {
		    final byte[] ct = Utils.copy(cyphered, i * 16 + offset, 16);

		    crypto.decrypt(ct, pt);
		    xor(pt, iv);
		    iv = Utils.copy(ct);
		    Utils.copy(paddedplain, i * 16, pt, 0, 16);
		}

		int padlen = paddedplain[paddedplain.length - 1];

		if (padlen <= 0 || padlen > 16) {
		    //#ifdef DEBUG
		    debug.error("decryptData, wrong padlen: " + padlen);
		    //#endif
		    throw new CryptoException();
		}

		plainlen = enclen - padlen;
		plain = new byte[plainlen];

		Utils.copy(plain, 0, paddedplain, 0, plainlen);

        //#ifdef DBC
        Check.ensures(plain != null, "null plain");
        Check.ensures(plain.length == plainlen, "wrong plainlen");
        //#endif
        return plain;
    }


    public byte[] encryptDataIntegrity(final byte[] plain) {

        byte[] sha = SHA1(plain);
        byte[] plainSha = Utils.concat(plain, sha);

        //#ifdef DBC
        Check.asserts(sha.length == DIGEST_LENGTH, "sha.length");
        Check.asserts(plainSha.length == plain.length
                + DIGEST_LENGTH, "plainSha.length");
        //#endif

        //#ifdef DEBUG
        debug.trace("encryptDataIntegrity plain: " + plain.length);
        debug.trace("encryptDataIntegrity plainSha: " + plainSha.length);
        //#endif

        return encryptData(plainSha, 0);
    }
    
    
    public byte[] decryptDataIntegrity(final byte[] cyphered)
            throws CryptoException {
        byte[] plainSha = decryptData(cyphered, 0);
        byte[] plain = Utils.copy(plainSha, 0, plainSha.length
                - DIGEST_LENGTH);
        byte[] sha = Utils.copy(plainSha, plainSha.length
                - DIGEST_LENGTH, DIGEST_LENGTH);
        byte[] calculatedSha = SHA1(plainSha, 0, plainSha.length
                - DIGEST_LENGTH);

        //#ifdef DBC
        //Check.asserts(SHA1Digest.DIGEST_LENGTH == 20, "DIGEST_LENGTH");
        Check.asserts(
                plain.length + DIGEST_LENGTH == plainSha.length,
                "plain.length");
        Check.asserts(sha.length == DIGEST_LENGTH, "sha.length");
        Check.asserts(calculatedSha.length == DIGEST_LENGTH,
                "calculatedSha.length");
        //#endif

        if (Arrays.equals(calculatedSha, sha)) {
            //#ifdef DEBUG
            debug.trace("decryptDataIntegrity: sha corrected");
            //#endif
            return plain;
        } else {
            //#ifdef DEBUG
            debug.error("decryptDataIntegrity: sha error!");
            //#endif
            throw new CryptoException();
        }
    }


}
