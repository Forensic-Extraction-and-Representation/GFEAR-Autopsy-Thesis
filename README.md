# GFEAR-Autopsy Plugin

This repository contains the GFEAR-Autopsy plugin which is a NetBeans-based module that integrates the FEAR Framework with the Autopsy digital forensics platform. **This is a static archive of the plugin codebase as it appears in the accompanying PhD thesis and is not actively maintained.**

## Overview

The GFEAR-Autopsy plugin enables forensic investigators to:

- Submit artefacts extracted by Autopsy to the FEAR Framework
- Query and visualise semantic knowledge graphs constructed from forensic evidence
- Interact with neuro-symbolic AI agents for natural language analysis
- Access the FEAR.WASM Web UI interface directly from Autopsy

## Components

- **Java-based NetBeans module** using the Autopsy plugin API
- **Embedded browser** powered by Java Chromium Embedded Framework (JCEF)
- **Integration layer** for bidirectional communication with the FEAR Framework API
- **About dialog** crediting the development team

## System Requirements

The reference setup uses:

- **Autopsy** (version 4.23.1 or later)
- **Java Development Kit** (JDK 17 or equivalent OpenJDK)
- **Java Chromium Embedded Framework** (JCEF build 1.0.70, windows-amd64)
- **FEAR Framework** (running and accessible via HTTPS at configurable local domain, default: `fear.local`)

## Installation

1. Obtain the pre-compiled plugin file: `com-fearlanguage-gfear.nbm`
2. Open Autopsy → **Tools** → **Plugins**
3. Select the **Downloaded** tab
4. Click **Add Plugins…** and select the `.nbm` file
5. Follow the installation prompts (unsigned plugin warning is expected)
6. Restart Autopsy to activate the plugin

## Configuration

After installation, configure the plugin for the active case to point at your FEAR Framework instance:

- **Tools** → **Options** → **Knowledge Graph** tab
- **Base Path**: `https://ui.fear.local/` (or your configured domain)
- **Graph API**: `https://api.fear.local/` (or your configured domain)
- **Case Name**: The name of the case in the FEAR Framework
- **Allow Insecure**: Enable to trust self-signed certificates

For full setup details, see the accompanying thesis documentation.

## Usage

1. Create or open a case in Autopsy
2. Extract artefacts using standard Autopsy modules
3. Open the plugin: **Tools** → **FEAR** icon in the main toolbar
4. Use the **Artifact Data** tab to submit artefacts to the FEAR Framework
5. Query and visualise the knowledge graph through the embedded Web UI interface

## Related Projects

- [FEAR Framework](https://github.com/Forensic-Extraction-and-Representation/FEAR-Thesis): Core framework and DSL implementation

## Citation

This codebase is associated with the following PhD thesis:

> Korol, A. (2026). *A Framework for Mapping Artefacts to Knowledge Graph Representations for Digital Forensic Investigations*. PhD thesis, Edith Cowan University, Perth, Australia.

The repository represents the Autopsy plugin integration module described in the thesis, providing the interface between Autopsy and the FEAR Framework's knowledge graph construction and querying capabilities.

## Note

This repository is an archive snapshot matching the thesis evaluation
