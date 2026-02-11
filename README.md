# Java LocalServer

A lightweight, feature-rich HTTP server written in Java with support for static file serving, CGI script execution, file uploads, and session management.

## Features

- **Multi-port Support**: Listen on multiple ports simultaneously
- **Static File Serving**: Serve HTML, CSS, JavaScript, and other static assets
- **CGI Script Execution**: Run server-side scripts (bash, shell, etc.)
- **File Uploads**: Support for multipart form data with configurable size limits
- **Session Management**: Built-in session handling with automatic cleanup
- **Routing Configuration**: Flexible route configuration via JSON
- **Directory Listing**: Optional directory listing for file browsing
- **Error Pages**: Customizable error page templates
- **Request Parsing**: Robust HTTP request and multipart form data parsing
- **Non-blocking I/O**: Efficient NIO-based server architecture

## Project Structure

```
├── src/
│   ├── Main.java                 # Server entry point
│   ├── Server.java              # Main server implementation
│   ├── Config.java              # Configuration model
│   ├── ConfigLoader.java        # Configuration file loader
│   ├── Router.java              # Request routing logic
│   ├── CGIHandler.java          # CGI script execution
│   ├── JsonParser.java          # JSON parsing utility
│   ├── ErrorResponse.java       # Error response handler
│   ├── connection/
│   │   └── Connection.java      # Connection management
│   ├── http/
│   │   ├── HttpRequest.java     # HTTP request representation
│   │   ├── HttpResponse.java    # HTTP response builder
│   │   ├── RequestParser.java   # HTTP request parser
│   │   ├── MultipartParser.java # Multipart form data parser
│   │   ├── MimeTypes.java       # MIME type detection
│   │   └── Session.java         # Session management
│   └── utils/
│       └── Cookie.java          # Cookie handling
├── cgi-bin/
│   └── hello.sh                 # Example CGI script
├── www/
│   ├── index.html               # Home page
│   ├── login.html               # Login form
│   ├── form.html                # POST test form
│   ├── upload.html              # File upload form
│   ├── manage.html              # Upload manager
│   ├── dashboard.html           # Admin dashboard
│   ├── style.css                # Stylesheet
│   └── test.txt                 # Sample static file
├── uploads/                     # Directory for uploaded files
├── config.json                  # Server configuration
└── README.md                    # This file
```

## Configuration

The server is configured via `config.json`:

```json
{
  "host": "127.0.0.1",
  "ports": [8080, 8081],
  "client_max_body_size": 10485760,
  "error_pages": {
    "404": "error_pages/404.html",
    "500": "error_pages/500.html"
  },
  "routes": [
    {
      "path": "/uploads",
      "root": "uploads",
      "methods": ["GET", "DELETE"],
      "directory_listing": true,
      "index": "index.html"
    },
    {
      "path": "/",
      "root": "www",
      "methods": ["GET", "POST"],
      "directory_listing": false,
      "index": "index.html"
    },
    {
      "path": "/cgi-bin",
      "root": "cgi-bin",
      "methods": ["GET", "POST"],
      "is_cgi": true
    }
  ]
}
```

### Configuration Options

- **host**: Listening address (e.g., "127.0.0.1" for localhost or "0.0.0.0" for all interfaces)
- **ports**: Array of port numbers to listen on
- **client_max_body_size**: Maximum request body size in bytes
- **error_pages**: Mapping of HTTP status codes to error page files
- **routes**: Array of route configurations with:
  - **path**: URL path pattern
  - **root**: Filesystem directory
  - **methods**: Allowed HTTP methods (GET, POST, DELETE, etc.)
  - **directory_listing**: Enable/disable directory browsing
  - **index**: Default file for directory requests
  - **is_cgi**: Whether this route executes CGI scripts

## Building and Running

### Prerequisites

- Java 11 or higher
- Bash (for CGI script execution)

### Compile

```bash
javac -d bin src/**/*.java
```

### Run

```bash
cd bin
java src.Main
```

The server will start and display:
```
Listening on 8080
Listening on 8081
```

## API Endpoints

### Default Routes

- `GET /` - Serves the home page
- `GET /form.html` - POST form test page
- `GET /upload.html` - File upload form
- `GET /login` - Login page
- `GET /manage.html` - Upload management page
- `GET /uploads` - List uploaded files (with directory listing)
- `POST /cgi-bin/hello.sh` - Execute CGI script

### File Upload

Submit a POST request to any route with method POST enabled:

```html
<form method="POST" enctype="multipart/form-data">
  <input type="file" name="file">
  <button type="submit">Upload</button>
</form>
```

## CGI Scripts

CGI scripts should:

1. Start with a shebang line (`#!/bin/bash`)
2. Output HTTP headers followed by a blank line
3. Output the response body

Example:
```bash
#!/bin/bash
echo "Content-Type: text/html"
echo ""
echo "<h1>Hello from CGI!</h1>"
```

## Features in Detail

### Session Management

- Sessions are automatically created for each client
- Session data persists across requests
- Inactive sessions are automatically cleaned up (5-minute timeout)
- Sessions are transmitted via cookies

### Request Parsing

- Full HTTP/1.1 request parsing
- Multipart form data support for file uploads
- Cookie parsing and management
- Query string parsing

### Error Handling

- Custom error pages for HTTP status codes
- Comprehensive error logging
- Graceful handling of malformed requests

## Development

Key classes and their responsibilities:

- **Server**: Main event loop using Java NIO Selectors
- **RequestParser**: Parses HTTP requests and headers
- **Router**: Routes requests to appropriate handlers
- **CGIHandler**: Executes and manages CGI processes
- **MultipartParser**: Handles multipart form data parsing
- **Session**: Manages client sessions and cookies

## License

This project is part of the java-localserver repository.

## Troubleshooting

### Port Already in Use

If you get a "port already in use" error, the configured port is already occupied. Either:
- Change the port in `config.json`
- Kill the process using the port: `lsof -i :PORT_NUMBER`

### CGI Script Not Executing

Ensure the script file is:
- Readable and executable (`chmod +x script.sh`)
- Located in the configured CGI root directory
- Using the correct shebang line

### File Upload Issues

Check the `client_max_body_size` in config.json matches your expected file sizes.

## Future Enhancements

- HTTPS/SSL support
- WebSocket support
- Caching mechanisms
- Compression (gzip)
- Load balancing across multiple ports
