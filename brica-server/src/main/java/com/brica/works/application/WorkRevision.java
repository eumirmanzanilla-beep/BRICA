package com.brica.works.application;

import com.brica.works.domain.Work;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Content validator for the existing representation, without schema changes. */
public final class WorkRevision {
    private WorkRevision() { }

    public static String etag(Work work) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            Object[] values = {work.getId(), work.getClient().getId(), work.getName(),
                    work.getLocation(), work.getDescription(), work.getStartDate(), work.getEndDate()};
            for (Object value : values) {
                // Distinguish null and delimit values by length to avoid ambiguous concatenation.
                String text = value == null ? "N" : "V" + value;
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return "\"" + HexFormat.of().formatHex(digest.digest()) + "\"";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }
}
