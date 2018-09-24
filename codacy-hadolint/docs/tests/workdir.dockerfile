##Patterns: DL3006, DL3003

##Info: DL3006
FROM debian
COPY package.json usr/src/app
##Info: DL3003
RUN cd /usr/src/app || exit
CMD ["npm", "start"]