# Product

<!-- impeccable:product-schema 1 -->

## Platform

adaptive

## Users

Primary users are administrative and operations staff within a construction company. They manage client records, construction-work records, and the operational documents associated with those works.

## Product Purpose

BRICA centralizes the information, files, and document configuration used to operate a construction company. A construction work is the central record through which staff manage its client relationship, operational information, authorized source attachments, and associated document configuration.

## Positioning

BRICA is an intentionally minimal, interaction-first internal application. Its workflows guide staff through the next appropriate step, making routine record and document work feel intuitive, smooth, pleasant, and elegant rather than like a generic administrative system.

## Operating Context

Staff use the desktop client with a Spring API to register and maintain clients and construction works; manage authorized source attachments; configure reference fields, DOCX templates, versions, and packages; and review extraction candidates before confirming data. The client does not connect directly to the database.

## Capabilities and Constraints

- The available system includes authenticated, authorized operations for clients and construction works; soft deletion, concurrency controls, and source attachment management are part of the established product behavior.
- Document configuration supports reference fields, DOCX templates, versioning, packages, and resolved values. Source extraction requires human review before confirmation.
- Output-document preview, definitive generation, and export are not currently available product capabilities.
- The desktop client must keep network and long-running work off the JavaFX UI thread. The server owns validation, authorization, persistence, and managed file storage.
- PostgreSQL schema evolution is owned by Flyway; historical document versions must not be silently overwritten.

## Brand Commitments

The BRICA name and an interaction-first, minimalist, intuitive, smooth, pleasant, and elegant experience are durable commitments. No additional brand assets or rules were provided.

## Evidence on Hand

- `README.md` documents current capabilities, technical boundaries, and explicitly unavailable output-document features.
- `docs/informative/` contains early ERS, DAS, and data-dictionary material that describes intent but does not guarantee present implementation.
- No approved visual assets, testimonials, customer claims, benchmarks, or external proof materials are available for fabrication.

## Product Principles

1. Keep the construction work as the understandable center of related operational information.
2. Make routine progress self-evident through clear, guided interaction rather than extra explanation.
3. Preserve human review, authorization, and traceability where operational data and documents change.
4. Prefer focused, calm workflows over administrative complexity.
