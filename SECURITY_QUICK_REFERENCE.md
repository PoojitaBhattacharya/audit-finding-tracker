# Security Implementation Quick Reference
## Audit Finding Tracker - Developer Guide

---

## 🔐 Security Features Implemented

### 1. PII Masking (Automatic)
**What's Masked?**
- Credit Cards: `4532-1234-5678-9010` → `****-****-****-9010`
- SSN: `123-45-6789` → `***-**-6789`
- Email: `user@example.com` → `u***r@example.com`
- Phone: `(555) 123-4567` → `***-***-4567`
- Dates: `01/15/1990` → `**/**/****`

**Where It Works:**
- ✅ All JSON responses (Java backend)
- ✅ All AI service responses
- ✅ CSV exports
- ✅ Error messages

**For Developers:**
```java
// No special code needed - automatic!
// Just return sensitive data, it will be masked
public AuditFinding getFinding(Long id) {
    AuditFinding finding = service.getFinding(id);
    return finding;  // PII automatically masked in response
}
```

---

### 2. XSS Protection (Input Validation)

**Blocked Patterns:**
- HTML tags: `<script>`, `<iframe>`, `<img onerror=>`
- JavaScript protocol: `javascript://`
- Event handlers: `onclick=`, `onerror=`, etc.
- Entity encoding: `&#x...`, `\uXXXX`

**For Developers (Python/AI Service):**
```python
# Use g.sanitized_data after request validation
@bp.route("/categorise", methods=["POST"])
def categorise():
    data = g.sanitized_data  # Already validated!
    text = data["text"]      # Safe to use
    # ... your logic ...
```

**For Developers (Java Backend):**
```java
// Use OutputEncodingUtil for dynamic HTML generation
String userInput = request.getParameter("search");

// Option 1: Encode before storing in HTML
String safe = OutputEncodingUtil.encodeForHtml(userInput);
model.addAttribute("searchTerm", safe);  // Safe in Thymeleaf

// Option 2: Encode for HTML attributes
String safeAttr = OutputEncodingUtil.encodeForHtmlAttribute(userInput);
// <input value="<%=safeAttr%>"> - safe

// Option 3: Encode for JavaScript context
String safeJS = OutputEncodingUtil.encodeForJavaScript(userInput);
// <script>var search = "<%=safeJS%>";</script> - safe
```

---

### 3. Security Headers (Automatic)

**Headers Applied by Nginx:**
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'; ...
Cache-Control: no-store, no-cache, must-revalidate
```

**What They Prevent:**
- ✅ MIME sniffing attacks
- ✅ Clickjacking (framing attacks)
- ✅ XSS via CSP violations
- ✅ Inline JavaScript/CSS
- ✅ Unsafe eval() usage
- ✅ Caching of sensitive data

**For Developers:**
- No changes needed! Headers are applied globally
- If you need to allow specific external resources:
  1. Modify CSP in `nginx.conf`
  2. Modify CSP in `SecurityConfig.java` (Java)
  3. Modify CSP in `app.py` (Python)
  4. Test thoroughly before enabling

---

### 4. Secure Cookies

**Automatic Configuration:**
```
HttpOnly: true       (prevents JavaScript access)
Secure: false        (set to true with HTTPS in production)
SameSite: Strict     (prevents CSRF cookie submission)
```

**For Developers:**
```java
// Don't store sensitive data in cookies
// Use encrypted JWT tokens instead
// JWT is automatically validated by JwtAuthenticationFilter

// In SecurityConfig.java, SessionCreationPolicy is STATELESS
// Cookies are NOT used for sessions (JWT only)
```

---

### 5. File Access Blocking (Automatic)

**Blocked Files/Directories:**
- `.git/`, `.github/` - Version control
- `.env`, `.env.*` - Environment variables
- `pom.xml`, `package.json` - Package configs
- `docker-compose.yml`, `Dockerfile` - Deployment configs
- `*.bak`, `*.sql` - Backup files
- Hidden files (starting with `.`)

**For Developers:**
```bash
# When adding new config files, add to .dockerignore
echo "myconfig.conf" >> .dockerignore

# When deploying, verify nginx.conf blocks them
curl http://localhost/myconfig.conf  # Should return 403
```

---

## 🚀 Best Practices for Developers

### 1. Input Validation
```python
# Python - Always use POST with validated body
@app.route("/api/search", methods=["POST"])
def search():
    data = g.sanitized_data  # Safe input
    query = data["query"]    # Already validated
```

```java
// Java - Use request body for sensitive data
@PostMapping("/findings/search")
public ResponseEntity<?> search(@RequestBody SearchRequest req) {
    String query = req.getQuery();  // Validated by Spring
    // ...
}
```

### 2. Output Encoding
```java
// Always encode when injecting user data into templates
String userComment = getUserComment();

// For HTML content
response.getWriter().write("<p>" + OutputEncodingUtil.encodeForHtml(userComment) + "</p>");

// For HTML attributes
response.getWriter().write("<input value=\"" + OutputEncodingUtil.encodeForHtmlAttribute(userComment) + "\">");

// For JavaScript context
response.getWriter().write("<script>var comment = \"" + OutputEncodingUtil.encodeForJavaScript(userComment) + "\";</script>");
```

### 3. Avoid These Anti-Patterns
```java
// ❌ DON'T: Concatenate user input directly
String dangerous = "<p>" + userInput + "</p>";  // XSS risk!

// ✅ DO: Encode before output
String safe = "<p>" + OutputEncodingUtil.encodeForHtml(userInput) + "</p>";

// ❌ DON'T: Trust GET parameters for sensitive data
@GetMapping("/transfer")
public void transfer(@RequestParam String fromAccount, @RequestParam String amount) {
    // From/amount are logged and cached!
}

// ✅ DO: Use POST body for sensitive data
@PostMapping("/transfer")
public void transfer(@RequestBody TransferRequest req) {
    String fromAccount = req.getFromAccount();
    String amount = req.getAmount();
}
```

---

## 🔍 Testing Security Features

### 1. Test XSS Protection
```bash
# This should be rejected (returns error)
curl -X POST http://localhost/findings \
  -H "Content-Type: application/json" \
  -d '{"title": "<script>alert(1)</script>"}'

# Response should contain: "error": "Unsafe input detected"
```

### 2. Test PII Masking
```bash
# Create a finding with CC number
curl -X POST http://localhost/findings \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"description": "CC: 4532-1234-5678-9010"}'

# Response should show CC as: "****-****-****-9010"
```

### 3. Test File Blocking
```bash
# All these should return 403/404
curl http://localhost/.env
curl http://localhost/.git
curl http://localhost/pom.xml
curl http://localhost/docker-compose.yml
```

### 4. Test Security Headers
```bash
# View response headers
curl -I http://localhost/findings

# Look for:
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# Content-Security-Policy: default-src 'self'...
# Cache-Control: no-store...
```

---

## 🐛 Debugging Security Issues

### Issue: CSP Violations in Browser Console
**Solution:**
1. Check which resource is blocked
2. Add to CSP in appropriate config file:
   - Nginx: `nginx.conf`
   - Java: `SecurityConfig.java`
   - Python: `app.py`
3. Example: If Google Fonts is blocked
   ```
   font-src 'self' https://fonts.googleapis.com;
   ```

### Issue: XSS Input Rejected
**Solution:**
1. Check sanitizer logs: `[SECURITY] XSS vector detected`
2. Review rejected input for HTML tags or scripts
3. If legitimate, update ALLOWED_PATTERN in `sanitizer.py`
4. Always get security team approval before widening patterns

### Issue: PII Not Masked in Export
**Solution:**
1. Verify ResponseMasker is integrated in response handler
2. Check if export endpoint returns proper content-type
3. Add fallback masking in service layer if needed

### Issue: Sensitive File Still Accessible
**Solution:**
1. Verify nginx.conf location blocks are correct
2. Reload nginx: `docker exec audit-tracker-nginx nginx -s reload`
3. Clear browser cache
4. Test with: `curl -I http://localhost/filename`

---

## 📚 Configuration Locations

| Feature | Java | Python |
|---------|------|--------|
| PII Masking | `PiiMaskingFilter.java` | `response_masker.py` |
| Input Validation | `@PreAuthorize` annotations | `sanitizer.py` |
| Output Encoding | `OutputEncodingUtil.java` | (automatic in Flask) |
| Security Headers | `SecurityConfig.java` | `app.py` |
| Cookie Security | `application.properties` | (via nginx proxy) |
| File Blocking | `nginx.conf` | `nginx.conf` |

---

## 🚨 Security Review Checklist (Before Commit)

- [ ] No new endpoints using GET for sensitive data
- [ ] All user input validated with `sanitize_all_fields()` or `@Valid`
- [ ] All dynamic output encoded with appropriate method
- [ ] No new `<script>` tags or inline event handlers
- [ ] No hardcoded credentials or API keys
- [ ] `.env` files added to `.gitignore`
- [ ] New files added to `.dockerignore` if sensitive
- [ ] No disabling of security filters or headers
- [ ] No `debug=True` in production config
- [ ] Third-party dependencies have no known CVEs

---

## 📞 Support & Questions

1. **CSP Violations?** Check browser DevTools Console
2. **Input Rejected?** Review security logs and update patterns
3. **Performance Issues?** PII masking is minimal, check database
4. **Need to Bypass Security?** Don't - submit request to security team
5. **Found a Bug?** Report immediately to security team

---

**Last Updated:** May 12, 2026  
**Version:** 1.0
