---
name: BRICA
description: Sistema operativo de expedientes para gestión constructora.
colors:
  shell-ink: "#17313D"
  shell-deep: "#132A34"
  document-teal: "#1B7F7A"
  document-teal-deep: "#126762"
  attention-amber: "#E2B44B"
  workspace-mist: "#E8EFF1"
  surface-paper: "#F9FBFB"
  field-paper: "#FFFFFF"
  rule-blue: "#C8D5D8"
  text-ink: "#18313D"
  text-muted: "#5D727B"
  success-tint: "#EAF4F2"
  selection-tint: "#CFE6E2"
  danger: "#A9473E"
typography:
  display:
    fontFamily: "Segoe UI, Arial"
    fontSize: "24px"
    fontWeight: 700
  body:
    fontFamily: "Segoe UI, Arial"
    fontSize: "13px"
    fontWeight: 400
  label:
    fontFamily: "Segoe UI, Arial"
    fontSize: "11px"
    fontWeight: 700
rounded:
  control: "4px"
  surface: "8px"
spacing:
  control: "9px"
  compact: "12px"
  section: "22px"
  workspace: "24px"
components:
  button-primary:
    backgroundColor: "{colors.document-teal}"
    textColor: "{colors.field-paper}"
    rounded: "{rounded.control}"
    padding: "9px 13px"
  button-secondary:
    backgroundColor: "{colors.surface-paper}"
    textColor: "{colors.text-ink}"
    rounded: "{rounded.control}"
    padding: "9px 13px"
  surface:
    backgroundColor: "{colors.surface-paper}"
    rounded: "{rounded.surface}"
    padding: "22px"
---

# Design System: BRICA

## Overview

**Creative North Star: "Dossier de obra"**

BRICA is a calm, technical operating system for construction records. It treats each module as part of an active dossier: the shell establishes location, content establishes the record or action, and status establishes what requires attention. The experience is direct and restrained rather than decorative.

The visual language uses stable structural color, a distinct document action color, and a warm attention signal. Tables and lists remain dense enough for administrative work, while spacing and tonal surfaces preserve a readable hierarchy.

**Key Characteristics:**
- Persistent dark rail for orientation and session state.
- Cool paper-like work surfaces with thin technical rules.
- Teal reserved for primary action, active navigation and document progress.
- Semantically colored states; warnings and destructive actions do not borrow the primary action color.

## Colors

The palette is a cool construction dossier: dark structural ink frames pale archive surfaces, while teal and amber make work states legible at a glance.

### Primary
- **Document Teal:** the action and active-state color. It is reserved for work the user can perform and confirmed navigation.

### Secondary
- **Attention Amber:** used for keyboard focus and work requiring attention; it never becomes a decorative accent.
- **Danger Red:** used only for destructive actions and errors.

### Neutral
- **Structural Ink:** the dark navigation rail and high-contrast structural text.
- **Workspace Mist:** the cool blue-gray background that separates the working canvas from white data surfaces.
- **Paper Surface:** the near-white background for documents, forms and contained work.
- **Technical Rule:** thin borders and tabular divisions.

**The Action Color Rule.** Teal means an available primary action, an active route or a confirmed work state. It is not used as a generic highlight.

## Typography

**Display Font:** Segoe UI, Arial  
**Body Font:** Segoe UI, Arial  
**Label Font:** Segoe UI, Arial

**Character:** Clear, quiet and native to the Windows desktop environment. Weight and spacing establish hierarchy instead of novelty type treatments.

### Hierarchy
- **Display** (700, 24px): primary page and dialog titles.
- **Title** (700, 17px): operational sections and module headings.
- **Body** (400, 13px): instructions, tabular text and field values.
- **Label** (700, 11px): compact system metadata and table headers.

**The Plain Language Rule.** Actions name the job being done—such as “Registrar obra” or “Abrir obras”—rather than using generic verbs without context.

## Layout

The desktop shell has a fixed 210px orientation rail, a contextual header and a padded working canvas. Content uses tight action groups, generous separation between sections and a 22px default surface inset. Tables, list views and document forms fill the remaining vertical space before the user is asked to scroll.

## Elevation & Depth

Depth comes from tonal contrast and technical rules, not heavy shadows. The dark rail establishes the outer structure; paper surfaces sit over the mist workspace with one-pixel borders. Hover and selection use pale teal tints to preserve depth without creating floating-card clutter.

## Shapes

Controls use a compact 4px corner radius. Work surfaces use 8px corners only when they represent a contained document, table or focused task. Long rounded pills are reserved for short statuses, never for the overall application layout.

## Components

### Buttons
- **Shape:** compact rounded rectangle (4px).
- **Primary:** document teal background, white text and 9px by 13px internal padding.
- **Hover / Focus:** darkens on hover; focus is marked by amber, not by a second action-colored outline.
- **Secondary / Danger:** paper with a technical border for secondary actions; red only for irreversible or destructive actions.

### Cards / Containers
- **Corner Style:** contained work surfaces use 8px corners.
- **Background:** paper surface against a mist workspace.
- **Shadow Strategy:** none at rest; separation is tonal and bordered.
- **Border:** technical rule around contained work.
- **Internal Padding:** 22px for primary work surfaces.

### Inputs / Fields
- **Style:** white paper field with a technical rule and compact radius.
- **Focus:** teal border with amber focus indication.
- **Disabled:** reduced opacity without removing the control’s structural outline.

### Navigation
- **Style:** a dark structural rail; inactive routes use light cool text, hover uses a darker teal-blue field, and the active route uses document teal with white type.

### Status Strips
- **Style:** pale teal surface with dark teal text for contextual system feedback. Error and warning states keep their semantic colors.

## Do's and Don'ts

### Do:
- **Do** preserve the rail, contextual header and working canvas as separate orientation layers.
- **Do** keep data tables legible and comparison-friendly; use tint and rules rather than decorative cards.
- **Do** reserve teal for active navigation, primary action and confirmed document work.
- **Do** write recovery-oriented feedback that names the affected task.

### Don't:
- **Don't** use a primary action color as a generic decorative accent.
- **Don't** turn every group of information into a card or add nested card stacks.
- **Don't** replace familiar JavaFX controls with ornamental stand-ins.
- **Don't** use color as the sole indicator of status, permission or failure.
