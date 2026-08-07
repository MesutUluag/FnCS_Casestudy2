package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LegacyStoreManagerGateway {

  private static final Logger logger = Logger.getLogger(LegacyStoreManagerGateway.class);

  public void createStoreOnLegacySystem(Store store) {
    // just to emulate as this would send this to a legacy system, let's write a temp file with the
    writeToFile(store);
  }

  public void updateStoreOnLegacySystem(Store store) {
    // just to emulate as this would send this to a legacy system, let's write a temp file with the
    writeToFile(store);
  }

  private void writeToFile(Store store) {
    try {
      // Step 1: Create a temporary file
      Path tempFile = Files.createTempFile(store.name, ".txt");
      logger.infof("Temporary file created at: %s", tempFile);

      // Step 2: Write data to the temporary file
      String content =
          "Store created. [ name ="
              + store.name
              + " ] [ items on stock ="
              + store.quantityProductsInStock
              + "]";
      Files.write(tempFile, content.getBytes());
      logger.info("Data written to temporary file.");

      // Step 3: Optionally, read the data back to verify
      String readContent = new String(Files.readAllBytes(tempFile));
      logger.infof("Data read from temporary file: %s", readContent);

      // Step 4: Delete the temporary file when done
      Files.delete(tempFile);
      logger.info("Temporary file deleted.");

    } catch (IOException e) {
      logger.errorf(e, "Failed to write store '%s' to temporary file", store.name);
      throw new RuntimeException("Store persistence to legacy system failed", e);
    }
  }
}
