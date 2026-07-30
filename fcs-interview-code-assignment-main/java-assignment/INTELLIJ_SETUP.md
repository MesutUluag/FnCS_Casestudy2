# IntelliJ IDEA Setup Guide

## Prerequisites
- IntelliJ IDEA (Community or Ultimate Edition)
- JDK 17+ installed
- Docker Desktop running

## Steps to Import and Run

### 1. Import Project
1. Open IntelliJ IDEA
2. Click **File** → **Open**
3. Navigate to: `/Users/mesutuluag/Downloads/FnCS_Casestudy2/fcs-interview-code-assignment-main/java-assignment`
4. Click **OK**
5. Wait for IntelliJ to import the Maven project and download dependencies

### 2. Configure Maven Settings (Important!)
Since your system has a corporate Maven repository configured, you need to use the custom settings file:

1. Go to **IntelliJ IDEA** → **Settings** (macOS) or **File** → **Settings** (Windows/Linux)
2. Navigate to **Build, Execution, Deployment** → **Build Tools** → **Maven**
3. In **User settings file**, click the checkbox **Override**
4. Browse and select: `/Users/mesutuluag/Downloads/FnCS_Casestudy2/fcs-interview-code-assignment-main/java-assignment/temp-settings.xml`
5. Click **Apply** and **OK**

### 3. Set Project SDK
1. Go to **File** → **Project Structure**
2. Under **Project Settings** → **Project**
3. Set **SDK** to Java 17 (or higher)
4. Set **Language level** to 17
5. Click **OK**

### 4. Enable Annotation Processing
1. Go to **Settings** → **Build, Execution, Deployment** → **Compiler** → **Annotation Processors**
2. Check **Enable annotation processing**
3. Click **Apply** and **OK**

### 5. Mark Generated Sources (After first build)
After running the project once, you need to mark the generated sources:

1. Right-click on `target/generated-sources/openapi` folder
2. Select **Mark Directory as** → **Generated Sources Root**
3. Right-click on `target/generated-sources/annotations` folder
4. Select **Mark Directory as** → **Generated Sources Root**

### 6. Run the Application

#### Option A: Using Pre-configured Run Configuration
1. Look for the run configuration dropdown at the top right
2. Select **Quarkus Dev Mode**
3. Click the green **Run** button (▶)

#### Option B: Create New Maven Run Configuration
1. Click **Add Configuration** (or **Edit Configurations**)
2. Click **+** → **Maven**
3. Name: `Quarkus Dev Mode`
4. Working directory: `$PROJECT_DIR$`
5. Command line: `quarkus:dev`
6. Click **OK**
7. Click the green **Run** button (▶)

### 7. Access the Application
Once started, the application will be available at:
- **Main URL:** http://localhost:8080
- **Dev UI:** http://localhost:8080/q/dev
- **API Documentation:** http://localhost:8080/index.html

## Troubleshooting

### Generated Code Not Found
If you see compilation errors about missing classes (like `WarehouseResource` from OpenAPI):
1. Run Maven goal: `mvn clean compile` first
2. Then mark `target/generated-sources/openapi` as **Generated Sources Root**

### Docker Connection Issues
- Make sure Docker Desktop is running
- Quarkus will automatically start a PostgreSQL container using Testcontainers

### Port Already in Use
If port 8080 is already in use:
1. Stop any other application using port 8080
2. Or modify `src/main/resources/application.properties` and add:
   ```properties
   quarkus.http.port=8081
   ```

### Hot Reload Not Working
- Make sure you're running in Dev mode (`quarkus:dev`)
- Build the project: **Build** → **Build Project** (Cmd+F9 / Ctrl+F9)
- Changes should be picked up automatically

## Useful IntelliJ Features for Quarkus

### Quarkus Tools (Ultimate Edition)
If you have IntelliJ Ultimate, install the **Quarkus Tools** plugin:
1. **Settings** → **Plugins**
2. Search for "Quarkus Tools"
3. Install and restart

### Database View
View the PostgreSQL database:
1. Go to **View** → **Tool Windows** → **Database**
2. Click **+** → **Data Source** → **PostgreSQL**
3. Get connection details from console output when app starts
4. Example: `jdbc:postgresql://localhost:59021/quarkus`

## Quick Commands

### Run Tests
```bash
./mvnw test
```

### Clean and Rebuild
```bash
./mvnw clean package
```

### Run in JVM Mode
```bash
./mvnw quarkus:dev
```

## Notes
- The application uses **Live Coding** - changes to Java code are automatically recompiled
- Database schema is automatically created/updated on startup (Hibernate DDL)
- Sample data is loaded from `import.sql` on startup
