#Package Manager used:
  MAVEN

#Files Included:
  VideoPlayer.java - The one that does all the things.
  Downloader.java  - Utility for downloading.
  FilesList.java   - Used to store the loaded files in order.

## Capabilities
  i) Can display files from web through web-scraping.
  ii) Creates thumbnails as files are loaded.
  iii) Can load individual files, a folder, or files stored through a link.
  iv) Can download any files that is being displayed.
  v) Supports the following file formats: MP4, MP3, M4A, GIF, JPG, JPEG, and PNG.

##Limitations
  i) If you try to load from a link and the file doesn't have a direct access link and requires extra arguments it can't discern such links from direct links, and ends up rejecting them.
  ii) Takes longer to load as no. of files stored in the folder or links grows.
  iii) Rudimentary UI.

#Libraries Used
  i) JavaFX
  ii) Java Swing
