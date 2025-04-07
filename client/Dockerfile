# Use official Node.js image
FROM node:16-alpine

# Set the working directory
WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of the app
COPY . .

# Build the frontend (if needed)
RUN npm run build

# Expose the port your app is running on (change if different)
EXPOSE 3000

# Run the frontend
CMD ["npm", "start"]