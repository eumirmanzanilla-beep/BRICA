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
                    work.getLocation(), work.getDescription(), work.getStartDate(), work.getEndDate(),
                    work.getContractNumber(), work.getContractor(), work.getContractedAmount(),
                    work.getAmountBeforeTax(), work.getTaxAmount(), work.getActualEndDate(), work.getStatus(), work.getDocumentName()};
            for (Object value : values) {
                update(digest, value);
            }
            var additional = new java.util.TreeMap<>(work.getAdditionalValues());
            update(digest, additional.size());
            additional.forEach((key, value) -> { update(digest, key); update(digest, value); });
            return "\"" + HexFormat.of().formatHex(digest.digest()) + "\"";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }

    private static void update(MessageDigest digest, Object value) {
        // Length prefixes and explicit null markers keep both fields and map entries unambiguous.
        String text = value == null ? "N" : "V" + value;
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
