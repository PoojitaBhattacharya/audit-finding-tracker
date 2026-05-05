SECURITY.md — Tool-23: Audit Finding Tracker

MVP Project | Sprint: 14 April – 9 May 2026
Security Reviewer: Rakshitha M S

---

Team Sign-off

Role| Name| Sign-off
Java Developer 1| Ganesh (Hemanth Kumar H P)| 
Java Developer 2| Rakshitha N| 
Java Developer 3| Megharaj| 
AI Developer 1| Vaishnavi R Adike| 
AI Developer 2| Varsha V Nayak| 
AI Developer 3| Poojita Bhattacharya| 
Security Reviewer| Rakshitha MS| 

---

1. Threat Model

#| Threat| Risk Level| Mitigation Implemented
1| SQL Injection| HIGH| Spring Data JPA parameterized queries. No raw SQL used.
2| JWT Token Theft| HIGH| Stateless JWT with expiry (24h). HTTPS enforced in production.
3| Broken Authentication| HIGH| BCrypt password hashing via Spring Security.
4| Unauthorized Access| HIGH| JwtAuthFilter protects all endpoints except "/api/auth/**".
5| Prompt Injection (AI)| HIGH| Flask input sanitisation blocks malicious prompts.
6| Rate Limiting Attack| MEDIUM| flask-limiter (30 req/min, 10 req/min for heavy endpoints).
7| Sensitive Data Exposure| HIGH| No PII stored. Generic error responses.
8| XSS Attack| MEDIUM| React escapes output automatically.
9| Hardcoded Secrets| HIGH| All secrets via environment variables. ".env" ignored in git.
10| IDOR| MEDIUM| JWT + role-based access control (RBAC).
11| File Upload Risk| MEDIUM| File type and size validation (<10MB).
12| AI Cache Poisoning| LOW| Redis uses SHA256 hashed keys (no raw input).

---

2. Security Architecture

Browser (React)
    │
    ▼
Spring Boot Backend (JWT + SecurityFilterChain)
    │
    ├── PostgreSQL (JPA)
    ├── Redis (cache)
    └── Flask AI Service (Groq + ChromaDB)

Public Endpoints:

- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/refresh

Protected Endpoints:

- All "/api/findings/**"
- All "/api/admin/**"
- All "/api/files/**"

---

3. Security Tests Conducted

Authentication & Authorization

Test| Expected| Result
No JWT access| 401/403| ✅ PASS
Invalid JWT| 401| ✅ PASS
Expired JWT| 401| ✅ PASS
Valid JWT| 200| ✅ PASS
Role restriction| 403| ✅ PASS

---

Input Validation

Test| Expected| Result
Empty input| 400| ✅ PASS
SQL Injection| Blocked| ✅ PASS
Prompt Injection| Blocked| ✅ PASS
XSS Input| Escaped| ✅ PASS

---

Rate Limiting

Test| Expected| Result
>30 req/min| 429| ✅ PASS
Heavy endpoint abuse| Throttled| ✅ PASS

---

File Upload

Test| Expected| Result
>10MB file| 400| ✅ PASS
Invalid type| 400| ✅ PASS

---

Infrastructure

Test| Expected| Result
No secrets in code| Env vars used| ✅ PASS
".env" ignored| Yes| ✅ PASS
Docker isolation| Yes| ✅ PASS
Redis secure keys| Yes| ✅ PASS

---

4. Findings & Status

ID| Finding| Severity| Status
F-01| JWT in localStorage| MEDIUM| IN PROGRESS
F-02| No backend rate limiting| MEDIUM| IN PROGRESS
F-03| In-memory users| LOW| ACCEPTED

All Critical findings: FIXED ✅
All High findings: FIXED ✅

---

5. Residual Risks

Risk| Plan
Backend rate limiting| Add in next sprint
JWT storage| Move to httpOnly cookies
AI hallucination| Add validation layer
API key exposure| Rotate after demo

---

6. Security Checklist

- [x] No hardcoded secrets
- [x] ".env" ignored
- [x] Password hashing
- [x] JWT authentication
- [x] Input validation
- [x] SQL injection prevention
- [x] AI sanitisation
- [x] Secure error handling
- [ ] Backend rate limiting
- [ ] httpOnly cookies

---

7. Tools Used

- Postman
- Browser DevTools
- Docker logs
- OWASP Top 10
- OWASP ZAP

---

8. Conclusion

All critical and high vulnerabilities are resolved.
Remaining issues are minor and planned for improvement.

---

Prepared by: Rakshitha MS
Project: Tool-23 — Audit Finding Tracker
Date: 9 May 2026
