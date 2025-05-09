# Travel Nest - Travel Management System

## 🌟 Overview
Travel Nest is a comprehensive travel management system built with Spring Boot, providing a robust backend for managing trips, packages, bookings, payments, and user reviews. The system offers secure user authentication, role-based access control, and integrated email notifications.

## 🚀 Key Features

### User Management
- Secure authentication using JWT
- Role-based access control (ADMIN, MANAGER, TOURIST)
- Email verification system
- Password reset functionality
- User profile management

### Trip & Package Management
- Create and manage travel trips
- Bundle trips into comprehensive packages
- Advanced search and filtering capabilities
- Image handling for trip details
- Detailed trip information management

### Booking System
- Secure booking process for trips and packages
- Real-time availability checking
- Booking status management
- Email notifications for booking confirmations
- Booking history tracking

### Payment Processing
- Secure payment handling
- Payment status tracking
- Payment confirmation system
- Email receipts
- Payment history

### Review System
- User reviews for trips and packages
- Rating system implementation
- Review moderation by admins
- Review visibility control
- User-specific review management

### Email Notifications
- Booking confirmations
- Payment receipts
- Account verification
- Password reset
- Custom email templates

## 🛠️ Technical Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Security**: Spring Security with JWT
- **Database**: MySQL
- **ORM**: Spring Data JPA/Hibernate
- **Email**: JavaMailSender
- **Documentation**: OpenAPI/Swagger

### Key Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Security
- Spring Boot Starter Data JPA
- Spring Boot Starter Mail
- JSON Web Token (JWT)
- MySQL Connector
- Lombok
- Swagger/OpenAPI

## 📋 Prerequisites
- Java 17 or higher
- Maven 3.x
- MySQL 8.x
- SMTP server access (Gmail)

## 🔧 Setup and Installation

## 1. Clone the repository
bash
git clone https://github.com/yourusername/travel-nest.git

## 2. Configure environment variables
cp .env.example .env
## Update .env with your configurations

## 3.Build the project 
mvn clean install

## 4.Run the application
mvn spring-boot:run

## 🔐 Security
JWT-based authentication
Role-based access control
Password encryption
Secure email verification
Protected endpoints
## 📧 Email Features
Custom HTML templates
Automated notifications
Booking confirmations
Payment receipts
Account management emails
## 🔄 Database Schema
Users and Roles
Trips and Packages
Bookings and Payments
Reviews and Ratings
Email Verification Tokens
## 👥 Role-Based Access
Admin: Full system access
Manager: Trip and package management
Tourist: Booking and review capabilities
## 🛣️ Future Roadmap
Web application development
Multi-language support
Advanced analytics
Social media integration
## 🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## 👨‍💻 Author
[Assem Omar]
GitHub: [@Assemom]
LinkedIn: [Your LinkedIn]

## 🙏 Acknowledgments
Spring Boot Team
MySQL Community
All contributors and testers
