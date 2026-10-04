# ⚡ DevBuddy.ai — Build for a Friend

> **🎃 Hacktoberfest Weekend Challenge 1 of 5: "Build for a Friend"**  
> An open-source AI pair programmer and debugging companion crafted to rescue developer friends from late-night bug hell, cryptic stack traces, and imposter syndrome.

---

## 💡 The Story: Why I Built This for a Friend

Every developer has that friend who spends 4 hours debugging at 1:00 AM, only to discover a missing semicolon, an unhandled CORS header, or a `NullPointerException`. When they get stuck, they don't just need a cold stack trace parser—they need encouragement, clear root-cause explanations without corporate jargon, and a reminder that every senior engineer has been there.

**DevBuddy AI** was built specifically for that friend:
1. **Understands their vibe:** Whether they need a **Patient Senior Dev**, a **Playful Code Roast & Cheer**, or an **Explain Like I'm Five** breakdown.
2. **Diagnoses the real issue:** Explains *why* the bug occurred and provides the tested fix.
3. **Friendship Debug Voucher:** Generates a custom, redeemable friendship pass (e.g. *1x Emergency Discord Screen Share & Pizza On Me*) they can redeem whenever they're stuck.

---

## 🤖 Open-Source AI at its Core

Unlike apps that rely on proprietary black-box APIs, DevBuddy is powered entirely by **open-source AI**:
* **Local Inference via Ollama:** Supports **Qwen 2.5 Coder (7B/14B)**, **Llama 3.2**, **DeepSeek Coder**, and **Mistral** running 100% locally on your machine—free, private, and offline-capable.
* **Hugging Face Open Weights:** Optional cloud inference using open models hosted on the Hugging Face Inference API.
* **Smart Client-Side Fallback:** Built-in heuristic engine so the UI works out-of-the-box even before downloading model weights.

---

## 🏗️ Minimalist Architecture: The "Three Codes"

The entire project is intentionally designed with **zero external build tools, zero node_modules, and zero Maven/Gradle dependencies**:

| File | Tech | Role |
| :--- | :--- | :--- |
| **[`App.java`](App.java)** | Pure Java (JDK 11+) | Lightweight HTTP server built using native `com.sun.net.httpserver` and `java.net.http.HttpClient`. Serves the frontend and proxies AI requests. |
| **[`index.html`](index.html)** | Semantic HTML5 & JS | Responsive developer workbench, error preset chips, persona selector, and care package generator. |
| **[`style.css`](style.css)** | Modern CSS3 | Glassmorphic IDE aesthetic with ambient cyan/purple glow, monospace code formatting, and neon voucher tickets. |

---

## ✨ Features

- ☕ **4 Unique Pair-Programming Personas:**
  - *Patient Senior Dev:* Gentle, clear explanation and clean fix.
  - *Playful Code Roast & Cheer:* Good-natured banter followed by a rock-solid solution.
  - *Speed & Big-O Optimizer:* Refactoring and computational complexity tips.
  - *Explain Like I'm Five:* Demystifies arcane compiler errors into plain analogies.
- 🐛 **1-Click Error Presets:** Pre-loaded test cases for `NullPointerException`, `CORS Preflight Failed`, `TypeError: Cannot read properties of undefined (reading 'map')`, and `Segmentation Fault`.
- 🎟️ **Dev VIP Debug Ticket:** An interactive, copyable voucher signed with love that your friend can redeem whenever they need a real-life rubber duck.
- 📋 **1-Click Export:** Instantly copies the full care package formatted for Discord, Slack, or WhatsApp.

---

## 🚀 Getting Started

### Option 1: Instant Browser Test (No Setup Required)
Simply double-click **`index.html`** in your file manager to launch the workbench in your browser. All UI controls, error presets, and smart diagnostics will work immediately!

---

### Option 2: Full-Stack with Local Open-Source AI (Ollama + Java)

#### 1. Prerequisites
- **Java JDK 11+** installed (`java -version`)
- *(Optional for live local AI)* **[Ollama](https://ollama.com/)** installed

#### 2. Pull an Open-Source Coding Model
```bash
ollama run qwen2.5-coder
# OR: ollama run llama3.2
```

#### 3. Compile and Run the Java Server
```bash
# Navigate to project directory
cd friend-ai

# Compile and start
javac App.java
java App
```

*(On Java 11+, you can run it directly in one step with `java App.java`)*

#### 4. Open DevBuddy
Open your web browser and navigate to:
```
http://localhost:8080
```

---

## 📝 DEV.to Submission Summary

If submitting this for the **Hacktoberfest Weekend Challenge: Build for a Friend**:

* **Challenge:** Challenge 1 of 5 — Build for a Friend
* **Prompt:** Build a brand-new project with open-source AI at its core.
* **Post Title Idea:** *DevBuddy AI: How I Built an Open-Source AI Pair Programmer for My Best Friend (In Just 3 Files!)*
* **Key Tags:** `#hacktoberfest`, `#ai`, `#opensource`, `#java`

---

## 📄 License

MIT License • Built with ❤️ for friends who code.
