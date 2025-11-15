# jnodesOne

![Java](https://img.shields.io/badge/Java-17-informational)
![Maven](https://img.shields.io/badge/Build-Maven-blue)
![GUI](https://img.shields.io/badge/Type-Desktop%20(Swing)-brightgreen)

`jnodesOne` is a Java/Swing application for drawing, visualizing, and lightly monitoring network topologies using SNMP.

It lets you place nodes (routers, switches, servers, Wi-Fi APs, etc.), connect them with links, and then poll devices via SNMPv2/SNMPv3 to keep track of link and node state.

## Features

- Interactive network map with draggable nodes and link visualization
- SNMP-aware nodes and links (SNMPv2c & SNMPv3)
- Background link/node polling (SNMP4J)
- Optional encrypted SNMPv3 credentials
- JSON-based map saving/loading
- Icons for various network device types
- Native Java Swing GUI

## Project Structure

- `jnodes3clientse` — GUI (Main window, dialogs)
- `mapElements` — Models: Node, Link, Interfaces
- `dataGenerator` — SNMP pollers
- `dataManagement` — Save/load map JSON
- `tools` — Utilities (SNMP wrapper, AES encryption, logging)
- `message` — Data transfer objects
- `DB` — Simple in-memory helper

## Requirements

- Java **17**
- Maven **3.x**

## Build

```
mvn clean package
```

Resulting artifacts (in `target/`):

- `jnodesOne-jar-with-dependencies.jar`
- `jre-minimal/`
- `jnodesOne.exe` (if building on Windows)

## Run

```
java -jar target/jnodesOne-jar-with-dependencies.jar
```

Windows alternative:

```
target\jnodesOne.exe
```

## Map Files

- Example maps: `map.json`, `map_mine.json`
- Icons inside `/icons`

## SNMP Notes

- Supports SNMPv2c community or SNMPv3 (auth+priv).
- SNMP logic in `tools.Snmp`
- Pollers: `linkMaintainer`, `nodeMaintainer`
- Encrypted SNMPv3 fields supported.