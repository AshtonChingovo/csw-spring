# CSW Membership Management Automation System

This system was built for the **Council of Social Workers Zimbabwe (CSW)** to streamline and partially automate the yearly registration and membership renewal process for thousands of social workers. It intends to replace a highly manual, Excel-based workflow with an efficient and user-friendly interface.

## 🚩 Problem

Currently, CSW staff had to:

- Manually process hundreds of **emails** for membership renewals, weekly.
- Update multiple yearly Excel tracking sheets (TS).
- Manually generate new practicing numbers weekly.
- Send hundreds of headshot request emails manually.
- Download, rename, and organize hundreds of client headshots sent via email.
- Compile new Excel files + folders of images every week to send to a third-party card printing vendor.

This process is slow (currently takes a week), error-prone, and not scalable.

---

## ✅ Solution

I built a full-stack system with the following functionality:

### 🔁 Membership Renewal Automation

- **Process TS**: Analyses the master Google Sheets file to determine which clients are due for renewal.
- **Renewal UI**: Staff can search and select members to renew. Upon renewal:
  - The system copies client data from a previous year's sheet into the current year's sheet.
  - A new practicing number is auto-generated.
  - The client's row is updated on the live Google Sheet.
- **Email Headshot Request**: After renewal, the system sends a templated email with attachment requesting a headshot in the correct format.

### 🖼️ Image Processing & Card Preparation

- **Upload TS**: Upload the latest yearly TS (Excel format).
- **Extract Images**: Connects to CSW's Gmail inbox, downloads headshot images, maps emails to users in the TS, and displays a UI to review & clean data (delete invalid entries).
- **Generate CardPro Data**: Prepares data (Full Name, Email, Reg #, Practicing #, Photo Name) required by the card printing vendor.
- **Download Files**: Outputs a ZIP with a CardPro Excel file + renamed headshots ready for external printing.

---

## 💡 Highlights

- ✅ Automates **80%+ of staff's weekly tasks**
- ✉️ Integrates with **Gmail API** to pull emails and send templated replies
- 📁 Integrates with **Google Sheets API** for live TS editing and renewals
- 💬 UI supports human validation where needed
- 🔐 Secure, efficient, and designed for real-world organizational workflows

---

## 🛠️ Tech Stack

- **Backend**: Spring Boot (Java)
- **Frontend**: Angular
- **Authentication**: Google OAuth2
- **Email Integration**: Gmail API (Java client)
- **Data Management**: Google Sheets API
- **File Export**: Apache POI (for Excel), Zip utilities
- **Deployment**: Dockerized and hosted on DigitalOcean droplet

---

## 📂 Sample Flow

1. **Staff logs in via**  
2. **Processes TS to identify pending renewals**  
3. **Renews member → updates Google Sheet with new prac#**  
4. **Sends automated headshot request email**  
5. **Extracts weekly headshots from Gmail inbox**  
6. **Prepares clean data and downloadable ZIP for CardPro**

---

## 📌 Status

✅ In concept demo phase with CSW for the 2025 registration cycle  
🚀 Ready for review & modifications on feature requests
