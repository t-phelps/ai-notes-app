Notes-AI

Turn your notes into a connected system — not a pile of text.

Notes-AI is a full-stack application that automatically organizes your notes into meaningful groups using similarity analysis and AI. 
Instead of manually sorting or tagging everything, your notes naturally form “smart directories” based on what they’re actually about.

Built with a modern stack (Spring Boot + React), and integrated with AI, payments, and cloud storage, 
this project is designed to feel like a real product—not just a demo.

Why This Exists

Most note apps treat notes like isolated documents.

Notes-AI treats them like a network.

Every time you write something, the system analyzes it, connects it to related ideas, and builds a 
structure for you automatically. Over time, your notes evolve into a graph of knowledge instead of a folder of randomness.

Core Features:

📝 Note Management

Write and store notes seamlessly, with files backed by Google Drive through an rclone-powered storage layer.

🧠 Smart Directories (Automatic Organization)

No tags. No folders. No manual sorting.

Notes are automatically grouped based on similarity using:

Jaccard similarity scoring
Tokenized title comparison
A graph structure stored as an adjacency list in PostgreSQL

What you get:

Related notes clustered together
Isolated notes preserved when they don’t match anything
A system that improves as you add more content

🤖 AI Study Guide Generation

Turn your notes into structured study material with one click.

Generates downloadable study guides using the OpenAI API
Designed for summarization, review, and learning
Usage is tied to subscription tiers

💳 Subscription System (Stripe Integration)

Fully implemented subscription flow with real-world behavior:
Multiple tiers (Basic, Pro, Premium)
Webhook handling for:
Payments
Renewals
Failures
Plan changes

🔐 Authentication & Security
JWT-based authentication (stateless)
Short-lived access tokens (15 min)
Refresh tokens (7 days, HttpOnly cookies)
Secure password reset via expiring hashed tokens

⚙️ Account Management
Users can:

Update passwords
Delete their account
View subscription status
Track purchase history

Tech Stack
Backend
Java 17 + Spring Boot 3
Spring Security + JWT
jOOQ + Flyway
PostgreSQL
Stripe API
OpenAI API
Gmail SMTP
rclone (Google Drive integration)

Frontend
React 19 + React Router v7
Bootstrap 5
Yup (validation)
Lucide React (icons)

System Architecture
frontend/      → React SPA (Azure Static Web Apps)
backend/       → Spring Boot API (Dockerized, Azure App Service)
PostgreSQL     → Relational database
Google Drive   → Note storage (via rclone server)
Stripe         → Billing + subscriptions
OpenAI         → AI-powered study guide generation
How Smart Directories Actually Work

This is the core idea behind the project.

When a note is created, a job is added to a queue (PENDING)
A scheduled service runs every 5 minutes
It compares the new note against all existing notes for that user
Similarity is calculated using Jaccard similarity on tokenized titles
If similarity > 0.3:
A weighted edge is created between the notes
Stored in a note_links adjacency list table
Duplicate relationships are ignored
Notes with no matches remain as standalone nodes
Jobs retry up to 3 times before failing
The frontend retrieves clusters by traversing this graph

Result:
Your notes become a dynamic, evolving graph of related ideas.

Environment Variables

To run this project, configure the following:

DB_URL
DB_USER
DB_PASSWORD
JWT_SECRET
SALTED_KEY
STRIPE_TEST_KEY
STRIPE_ENDPOINT_SECRET
FRONTEND_URL
RCLONE_USER
RCLONE_PASS
OPENAI_API_KEY
SMTP_LOGIN_USERNAME
SMTP_LOGIN_PASSWORD
RCLONE_CONFIG

Running Locally

Prerequisites
Java 17
Node.js
PostgreSQL
rclone (connected to Google Drive)
Docker (optional)

Start Backend
cd backend
mvn spring-boot:run

Start Frontend
cd frontend
npm install
npm start
Frontend → http://localhost:3000
Backend → http://localhost:8080
Database Migrations

Handled automatically by Flyway on application startup.

Migration files:

backend/src/main/resources/db/migration
Subscription Tiers
Tier	Price	Study Guide Generations
Basic	$5/mo	50
Pro	$10/mo	200
Premium	$25/mo	600

Deployment
Backend → Dockerized, deployed on Azure App Service
Frontend → Azure Static Web Apps
CI/CD → GitHub Actions (on push to main)


This project isn’t just about taking notes.

It’s about building a system that thinks with you—connecting ideas automatically so you can focus on writing, not organizing.
