##Patterns: DL3006

##Info: DL3006
FROM debian
COPY package.json usr/src/app
WORKDIR /usr/src/app \
RUN npm install node-static
CMD ["npm", "start"]