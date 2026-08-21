# Vaultiq Frontend Design System
**Version:** 1.0
**Aesthetic:** Premium Minimalism / Swiss Design
**Primary Identity:** Trust, Clarity, High-End Fintech

## 1. Core Principles
The Vaultiq platform is designed to look like a premium financial exchange rather than a traditional, visually noisy betting site. 
*   **Minimalism:** Extensive use of whitespace (negative space) to reduce cognitive load. 
*   **Restraint:** Colors are used exclusively for semantic meaning (win/loss) or to draw the eye to primary actions.
*   **Precision:** Crisp borders, sharp typography, and perfect alignment. It must feel like a precision tool.

## 2. Typography
Typography is the cornerstone of this design. Because there are no heavy graphical elements, the font carries the aesthetic weight.
*   **Primary UI Font (Numbers, Data, Odds):** `Geist` or `Inter`. Chosen for their exceptional tabular lining and geometric clarity. Odds must be instantly readable.
*   **Secondary Font (Headings, Branding - Optional):** `Instrument Serif` or `Playfair Display`. Used very sparingly for large numbers (e.g., wallet balance) or page titles to inject a sense of luxury and tradition into the modern interface.
*   **Weights:** Stick strictly to Regular (400) for data, and Medium/Semi-Bold (500/600) for headers. Avoid excessive bolding.

## 3. Color Palette

The system supports both Light and Dark modes. The defining feature is the stark monochrome background paired with a sharp **Sky Blue** accent.

### Brand Accent
*   **Primary Accent:** Sky Blue (`#0EA5E9`)
*   **Hover/Active State:** Ice Blue (`#38BDF8`)
*   *Usage:* "Place Bet" buttons, active navigation tabs, selected odds highlighting.

### Dark Mode (The "Black Card" Look)
*   **Background:** Deep Matte Charcoal (`#121212` or `#0A0A0A`). Absolutely no gradients; pure flat darkness.
*   **Panels/Cards:** Elevated Charcoal (`#1A1A1A`)
*   **Borders:** Subtle Grey (`#2A2A2A`) - barely visible, just enough to separate containers.
*   **Primary Text:** Off-White (`#F3F4F6`)
*   **Secondary Text (Labels, Headers):** Muted Grey (`#8E8E93`)

### Light Mode (The "Swiss Editorial" Look)
*   **Background:** Pristine White (`#FFFFFF`) or Alabaster (`#FAFAFA`)
*   **Panels/Cards:** Pure White (`#FFFFFF`)
*   **Borders:** Soft Grey (`#E5E7EB`)
*   **Primary Text:** Ink Black (`#0F1115`)
*   **Secondary Text:** Slate Grey (`#6B7280`)

### Semantic Colors
Used strictly for data status, never for decoration.
*   **Success / Profit:** Bright Mint Green (`#10B981`)
*   **Danger / Loss / Locked Market:** Crisp Crimson (`#F43F5E`)

## 4. UI Components & Styling

### Buttons
*   **Primary Action (e.g., Place Bet):** Solid Sky Blue background with white text. No shadows. Slight scale transformation on hover.
*   **Secondary Action:** Transparent background with a Sky Blue border, or simple monochrome styling. 

### Cards & Layout (Odds Grid)
*   No heavy drop shadows. Rely entirely on subtle 1px borders to separate rows and cards.
*   Cells containing odds should have generous padding. 
*   When a user selects an odd, the cell background should invert or highlight with the Sky Blue accent to provide immediate visual feedback.

### Micro-Animations
*   **Data Updates:** When an odd changes dynamically, the number should flash green (if increasing) or red (if decreasing) and smoothly fade back to white/black over 1.5 seconds.
*   **Hover States:** Interactive elements should respond instantly.

## 5. Technology Stack & Defaults
*   **Framework:** React and Next.js. This component-driven architecture is ideal for building the strict, reusable UI elements required by this minimal design system.
*   **Default Theme:** The application will default to **Dark Mode** on the initial load to provide the intended high-end, immersive betting experience immediately.
