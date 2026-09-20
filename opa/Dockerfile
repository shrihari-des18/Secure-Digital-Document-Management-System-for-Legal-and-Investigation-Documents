FROM openpolicyagent/opa:latest

# Expose port 8181
EXPOSE 8181

# Copy Rego policies into the container
COPY policies /policies

# Run OPA as a server, binding to all interfaces on port 8181
# Note: --watch is omitted here for production images, but can be added back or overridden in dev
CMD ["run", "--server", "--addr", "0.0.0.0:8181", "--log-level=info", "/policies"]
