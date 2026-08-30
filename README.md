# Perch - Email Client

a dead simple IMAP mail client

## Recent Improvements

### 1. **Logging Infrastructure**
- Added comprehensive logging system using SLF4J
- Custom `Logger` utility class for consistent logging across the app
- Timestamped, level-based logging (DEBUG, INFO, WARN, ERROR)
- All major operations now log their lifecycle

### 2. **Error Handling & User Feedback**
- Created `AppError` data class for structured error representation
- Added `ErrorDialog` composable for displaying user-friendly error messages
- Error dialogs show error title, message, and technical details
- All critical operations now catch and display errors to users

### 3. **Coroutines & Threading**
- Replaced raw `Thread.start()` calls with Kotlin coroutines
- Implemented proper async/await pattern using `LaunchedEffect` and `withContext`
- Created `AsyncJob` utility for managing async operations with loading/error states
- All network operations now use `Dispatchers.IO` for proper async handling

### 4. **UI/UX Improvements**
- Added loading state indicators with `LoadingPane` component
- Improved empty state messaging (e.g., "No messages" when inbox is empty)
- Better error display in reading pane with error details
- Loading indicator in "Add Account" button
- Empty mail list now shows appropriate feedback

### 5. **Performance & Caching** (In Progress)
- Created `MailCache` utility for in-memory caching of mail summaries and content
- Reduces unnecessary network requests for content fetches
- Thread-safe concurrent cache implementation
- Cache statistics tracking

### 6. **Code Quality Improvements** (Planned)
- Better structured state management
- Improved code organization
- Reduced code duplication
- Better separation of concerns

### Key Components

**Logger** - Logging utility
- Provides structured logging with timestamps
- Integrates with SLF4J for file output
- Used throughout the app for operation tracking

**AppError** - Error representation
- Contains error title, message, and optional throwable
- Supports recoverable vs. unrecoverable errors

**AsyncJob** - Async operation management
- Handles loading/error/success states
- Automatic error handling and state cleanup

**MailCache** - Mail data caching
- In-memory cache for mail summaries and content
- Concurrent access safe

**ImapWatcher** - IMAP connection and monitoring
- Connects to IMAP servers
- Monitors new mail with IDLE protocol
- Handles reconnection with exponential backoff
- Comprehensive logging of all operations

## Features

- **Gmail Integration** - OAuth2 authentication with Gmail
- **IMAP Support** - Works with any IMAP server
- **Real-time Mail Monitoring** - IDLE protocol for instant notifications
- **System Tray** - Minimize to tray with unread count indicator
- **Multiple Accounts** - Add and manage multiple email accounts
- **HTML Rendering** - View HTML emails with security sanitization
- **Error Recovery** - Automatic reconnection on failures

## Configuration

1. Set up Google OAuth2 credentials at [Google Cloud Console](https://console.cloud.google.com)
2. Place `credentials.json` in the resources folder
3. Run the application - it will guide you through the OAuth2 flow

Note: You need your own credentials if you want to build it from source

## Logging

Logs are output to:
- Console (system out)
- SLF4J file appender (if configured)

Log levels:
- **DEBUG** - Detailed operational info
- **INFO** - General operational messages
- **WARN** - Warning conditions
- **ERROR** - Error conditions with stack traces

## Future Improvements

- [ ] Folder/label support (beyond INBOX)
- [ ] Email composition
- [ ] Search functionality
- [ ] Attachment handling
- [ ] Email threading
- [ ] Persistent storage/database
- [ ] Performance optimization with larger mailboxes
- [ ] Unit and integration tests
- [ ] Dark/light theme toggle
