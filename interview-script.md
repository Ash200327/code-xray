# 2-3 Minute Interview Presentation Script

This script is designed for you to read and practice before interviews. It explains your project and the deployment architecture in a clear, conversational, and professional way.

---

## Part 1: Project Overview & Features (Approx. 1.5 minutes)

**"Hi everyone, I’d like to walk you through my project: Code-Xray.**

**Code-Xray is a production-ready AI developer assistant for GitHub repositories. The main goal is to let developers talk directly to their codebases using natural language, helping them understand complex projects, find where functions are defined, or generate documentation quickly.**

**For the technology stack:**
* **On the frontend**, I used **React, TypeScript, Vite, and Tailwind CSS** to create a highly responsive, clean developer dashboard.
* **On the backend**, I built a reactive service using **Java 21, Spring Boot (WebFlux), and Spring AI** to handle requests asynchronously and communicate with OpenAI.
* **For storage**, I used **PostgreSQL** combined with the **pgvector** extension to handle both structured user data and vector embeddings for semantic search.

**How it works under the hood is a three-step pipeline:**
1. **Ingestion**: When a user inputs a repository URL, the backend clones it, walks the file structure, filters out junk files, and runs language-aware chunking (with custom rules for Java, JavaScript, and Markdown).
2. **Hybrid Retrieval**: When a user asks a question, the system does not just do a simple vector search. It runs a **hybrid search** combining vector embeddings and full-text keyword matching to find the most relevant context snippets in the database.
3. **Grounded Chat**: The assistant then feeds this context to the LLM (GPT-4o-mini) to generate a response, which is streamed back to the frontend in real-time. Crucially, the UI displays clear **citations** (source files, line numbers, and confidence scores) so developers can verify where the answer came from."

---

## Part 2: Deployment & Architecture (Approx. 1 minute)

**"For the deployment, I wanted to showcase modern, decoupled cloud architecture, so I migrated the application from a traditional VPS (like AWS EC2) to a 100% serverless and PaaS-based topology.**

**I split the application into three independent tiers:**
* **On the frontend**, I hosted the React SPA on **Vercel**, which serves our static assets globally via their Edge CDN.
* **On the database layer**, I migrated to **Neon Tech**, using a serverless PostgreSQL database with native `pgvector` index support.
* **On the backend**, I containerized the Spring Boot application using Docker and deployed it as a web service on **Render's Free Tier**.

**One of the key engineering challenges I solved was resource optimization for Render's free tier, which restricts containers to 512MB of RAM. A standard Spring Boot service running Java can easily consume more than that and crash.**

**To solve this, I did two things: first, I configured custom Java JVM flags (specifically setting `-Xmx300m` and setting `MaxRAMPercentage` to 60%) to strictly limit the heap memory. Second, I introduced a custom `vercel.json` routing configuration to manage client-side SPA path rewrites, preventing 404 errors when users refresh deep page routes."**

---

## Tips for Delivering This Script:
1. **Pacing:** Speak slowly and clearly. Pause for a second between sections.
2. **Confidence:** Focus heavily on **Part 2 (Deployment)**. Hiring managers love when junior developers understand system resource constraints (JVM memory limits), database decoupling, and static hosting redirects, as these are highly practical modern engineering skills.
3. **Demo Ready:** Keep your browser open to your Vercel deployment URL in another tab during your interview so you can share your screen and show them immediately if they ask!
