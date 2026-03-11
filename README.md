# ChitChat

A Java socket-based chat application with a client-server architecture.

## Project Structure

- `server/` - Chat server that handles all client connections and message routing
- `client/` - Client application for connecting to the server
- `shared/` - Shared classes used by both client and server (User, Conversation, Message, etc.)

## Features

- User registration and login
- Friend requests
- Group conversations
- Real-time messaging routed through the server

## How It Works

Clients connect to the server via TCP sockets. All messages are routed through the server — clients never communicate directly with each other. The server maintains a registry of connected clients and delivers messages to the correct conversation members.

## Running the App

1. Start the server (`ChatServer.java`)
2. Start one or more clients (`ChatClient.java`)
3. Register or login, then start chatting