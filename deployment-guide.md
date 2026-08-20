# Codebase Assistant - How Deployment Works (Simple Guide)

This guide explains how this project is hosted on the internet, using simple terms that are easy to understand and explain to others (especially in interviews!).

---

## 1. The Big Picture (How Everything Connects)

Think of your AWS EC2 server as a **virtual computer in the cloud**. Instead of running all parts of your project directly on that computer, we put them into three separate "boxes" called **Containers** using a tool called **Docker**.

We have three containers running at the same time:
1. **The Frontend (Nginx)**: The user interface (React) that you see in your browser.
2. **The Backend (Spring Boot)**: The "brain" of the app that talks to OpenAI and handles logins.
3. **The Database (PostgreSQL with pgvector)**: The memory that stores users, chats, and code search data.

We use **Docker Compose** as the team leader to start all three containers together and link them to a private internal network.

---

## 2. Terminology Explained Simply

Here is what the technical terms actually mean in plain English:

### What is Docker and a Container?
* **Real-world analogy:** Think of a shipping container. No matter what is inside (clothes, food, electronics), it can be loaded onto any ship in the world because the box has a standard shape.
* **In Software:** A container is a standard box containing your code and all the tools it needs to run. It guarantees that the code will run exactly the same way on your computer, on your friend's computer, or on an AWS server.

### What is Nginx?
* **Real-world analogy:** Think of Nginx as a **front-desk receptionist** in an office building.
* **In Software:** When a user visits your website (`http://52.62.45.74`), they talk to Nginx first. 
  * If they ask for the website pages, Nginx hands them the React files.
  * If they ask to do a chat query (`/api`), Nginx routes them to the Backend receptionist.
  * If they ask to update loading status (`/ws`), Nginx routes them to the WebSocket receptionist.

### What is a Reverse Proxy?
* This is the act of Nginx routing traffic. Instead of exposing the backend directly to the public internet, Nginx stands in front of it and forwards the traffic. It protects the backend and simplifies connections.

### What is CORS (and why Nginx fixed it)?
* **The Problem:** Browsers have a security rule: if a website is loaded from one place (like a Vercel URL), it is not allowed to talk to a backend at another place (like a Render URL) unless the backend explicitly allows it.
* **The Fix:** Because Nginx serves both the frontend files and proxies the backend under the exact same IP and port (`80`), the browser thinks they are the same location. CORS is bypassed automatically!

### What is an Elastic IP?
* **Real-world analogy:** Imagine if your home address changed every time you turned off your TV. That would be chaotic!
* **In Software:** AWS EC2 instances get a temporary IP address by default. If the server restarts, the IP changes. An **Elastic IP** is a permanent, static address that never changes, meaning your website link will never break.

---

## 3. The Two Big Decisions We Made (Interview Highlights)

When interviewers ask: *"What challenges did you face and how did you solve them?"*, you can mention these two points:

### Challenge A: The Server Ran Out of Memory (OOM)
* **The Issue:** Your AWS EC2 server has 1GB of RAM. Compiling Java code and building React code takes a lot of memory (usually more than 1.5GB). When we tried to build it on the server, the server ran out of memory and froze.
* **Our Solution (Local Pre-building):** We decided to build the frontend files (`dist` folder) and the backend jar file (`target/*.jar`) locally on your personal computer (which is powerful). We pushed these pre-built files to GitHub. The server's Docker containers were configured to just copy these pre-built files.
* **The Result:** The server no longer has to run heavy compilers. Building on the server now takes **under 5 seconds** and uses almost zero RAM!

### Challenge B: Merged Words in the Chat Stream
* **The Issue:** The browser's native event streaming tool (`EventSource`) has a built-in behavior where it automatically removes spaces at the start of incoming text lines. This caused the AI chat text to display with merged words (e.g. `is aStudent` instead of `is a Student`).
* **Our Solution (JSON Wrapping):** Instead of sending raw text tokens from the backend, we wrapped each token in a JSON object (like `{"text": " Student"}`). The frontend parses the JSON to extract the text.
* **The Result:** All spaces, list indentations, and newlines are preserved perfectly in the chat window.

---

## 4. How to Manage Your Server (Cheat Sheet)

If you ever want to check on things, log in using your key:
```powershell
ssh -i "$HOME\OneDrive\Desktop\code-xray-key.pem" ubuntu@52.62.45.74
```

Then run these simple commands inside the `code-xray` folder:

* **To see if your app is working or view logs:**
  ```bash
  sudo docker logs -f codeassistant-backend-prod
  ```
  *(Press `Ctrl + C` to stop viewing the logs).*

* **To stop the website completely:**
  ```bash
  sudo docker compose -f docker-compose.prod.yml down
  ```

* **To start the website back up:**
  ```bash
  sudo docker compose -f docker-compose.prod.yml up -d
  ```
