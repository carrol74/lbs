FROM node:18
WORKDIR /usr/src/app
COPY package*.json ./
COPY . .
RUN npm install
EXPOSE 8080
CMD ["npm", "start"]
RUN adduser -u 8877 ctf
USER ctf 
