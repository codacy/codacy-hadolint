##Patterns: DL3006, DL3003

##Warn: DL3006
FROM debian
COPY package.json usr/src/app
##Warn: DL3003
RUN cd /usr/src/app || exit
CMD ["npm", "start"]
