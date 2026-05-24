# WhatsApp Business Firewall

A lightweight, privacy-first Android app that blocks **WhatsApp Business** from accessing the internet using Android’s local VPN-based firewall method.

This project was built to solve one simple problem:

> I wanted WhatsApp Business to go fully offline — no Wi-Fi, no mobile data, no background connection — without rooting my phone.

## Overview

WhatsApp Business Firewall is a local-only Android firewall app designed to block internet access for WhatsApp Business on a non-rooted Android device.

The app uses Android’s `VpnService` API to create a local VPN tunnel on the phone. This allows the app to filter and block selected app traffic without needing root access.

Important: this is **not** a commercial VPN app.  
It does **not** connect to an external VPN server.  
All filtering happens locally on the device.

## Key Features

- Blocks WhatsApp Business internet access
- Works without root
- Blocks both Wi-Fi and mobile data
- Uses Android’s local `VpnService` firewall approach
- Keeps other apps working normally
- Lightweight and simple UI
- Privacy-first design
- No external server connection
- No analytics
- No cloud sync
- No account or login required
- No tracking

## Target App

The main blocked app is:

```txt
com.whatsapp.w4b
