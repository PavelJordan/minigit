package cz.cuni.mff.jordanpa.minigit.structures;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@ParameterizedClass
@ValueSource(strings = {"", "abc", "def", "ghi", "a", "a\na\n"})
public class BlobTest {

    @Parameter
    public String data;

    @TempDir
    Path tempDir;

    Path stringDataPath;
    Path binaryDataPath;
    Path scratchPath;

    @BeforeEach
    void init() {
        stringDataPath = tempDir.resolve("stringData");
        binaryDataPath = tempDir.resolve("binaryData");
        scratchPath = tempDir.resolve("scratch");
        try {
            Files.writeString(stringDataPath, data);
            Files.write(binaryDataPath, createBinaryData(data.getBytes()));
            Files.createFile(scratchPath);
        }
        catch (IOException e) {
            fail("IOException while creating temporary file");
        }
    }

    @Test
    void blobConstructorWithStringDataSavesCorrectly() {
        Blob blob = new Blob(data.getBytes());
        try (BufferedInputStream reader = blob.getContentReader()) {
            String contents = new String(reader.readAllBytes());
            assertEquals(data, contents);
        } catch (IOException e) {
            fail("IOException while reading blob contents");
        }
    }

    @Test
    void blobSHA1SameForSameStringContentFromConstructor() {
        Blob blob1 = new Blob(data.getBytes());
        Blob blob2 = new Blob(data.getBytes());
        assertEquals(blob1.miniGitSha1(), blob2.miniGitSha1());
    }

    @Test
    void blobConstructorWithBinaryDataSavesCorrectly() {
        byte[] binaryData = createBinaryData(data.getBytes());
        Blob blob = new Blob(binaryData);
        try (BufferedInputStream reader = blob.getContentReader()) {
            byte[] contents = reader.readAllBytes();
            assertArrayEquals(binaryData, contents);
        } catch (IOException e) {
            fail("IOException while reading blob contents");
        }
    }

    @Test
    void blobDoesNotFailWhenGivingLinesForBinaryDataFromConstructor() {
        byte[] binaryData = createBinaryData(data.getBytes());
        Blob blob = new Blob(binaryData);
        try {
            blob.readAllLines();
        } catch (IOException e) {
            fail("IOException while reading blob binary contents as lines. Should not throw exception.");
        }
    }

    @Test
    void blobCorrectlyCreatedWithStringDataFromPath() {
        Blob blob;
        try {
            blob = new Blob(stringDataPath);
        }
        catch (IOException e) {
            fail("IOException while creating blob from file");
            return;
        }
        try (BufferedInputStream reader = blob.getContentReader()) {
            String contents = new String(reader.readAllBytes());
            assertEquals(data, contents);
        } catch (IOException e) {
            fail("IOException while reading blob contents");
        }
    }

    @Test
    void blobCorrectlyCreatedWithBinaryDataFromPath() {
        byte[] binaryData = createBinaryData(data.getBytes());
        Blob blob;
        try {
            blob = new Blob(binaryDataPath);
        }
        catch (IOException e) {
            fail("IOException while creating blob from file");
            return;
        }
        try (BufferedInputStream reader = blob.getContentReader()) {
            byte[] contents = reader.readAllBytes();
            assertArrayEquals(binaryData, contents);
        } catch (IOException e) {
            fail("IOException while reading blob contents");
        }
    }

    @Test
    void blobDoesNotFailWhenGivingLinesForBinaryFiles() {
        Blob blob;
        try {
            blob = new Blob(binaryDataPath);
        }
        catch (IOException e) {
            fail("IOException while creating blob from file");
            return;
        }
        try {
            blob.readAllLines();
        } catch (IOException e) {
            fail("IOException while reading blob binary contents as lines. Should not throw exception.");
        }
    }

    @Test
    void blobCorrectlySavedWithStringDataIntoFiles() {
        Blob blob = new Blob(data.getBytes());
        try {
            blob.writeContentsTo(scratchPath);
            String contents = new String(Files.readAllBytes(scratchPath));
            assertEquals(data, contents);
        }
        catch (IOException e) {
            fail("IOException while writing blob to file");
        }
    }

    @Test
    void blobCorrectlySavedWithBinaryDataIntoFiles() {
        Blob blob = new Blob(createBinaryData(data.getBytes()));
        try {
            blob.writeContentsTo(scratchPath);
            byte[] contents = Files.readAllBytes(scratchPath);
            assertArrayEquals(createBinaryData(data.getBytes()), contents);
        }
        catch (IOException e) {
            fail("IOException while writing blob to file");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "def", "ghi", "a", "a\na\n"})
    void shaEqualsOnlyWhenStringDataMatchAfterConstruction(String otherData) {
        Blob blob1 = new Blob(data.getBytes());
        Blob blob2 = new Blob(otherData.getBytes());
        if (data.equals(otherData)) {
            assertEquals(blob1.miniGitSha1(), blob2.miniGitSha1());
        }
        else {
            assertNotEquals(blob1.miniGitSha1(), blob2.miniGitSha1());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "def", "ghi", "a", "a\na\n"})
    void shaEqualsOnlyWhenBinaryDataMatchOnConstruction(String otherData) {
        Blob blob1 = new Blob(createBinaryData(data.getBytes()));
        Blob blob2 = new Blob(createBinaryData(otherData.getBytes()));
        if (data.equals(otherData)) {
            assertEquals(blob1.miniGitSha1(), blob2.miniGitSha1());
        }
        else {
            assertNotEquals(blob1.miniGitSha1(), blob2.miniGitSha1());
        }
    }

    private byte[] createBinaryData(byte[] dataBinaryFirst) {
        byte[] dataBinarySecond = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        byte[] result = new byte[dataBinaryFirst.length + dataBinarySecond.length];
        System.arraycopy(dataBinaryFirst, 0, result, 0, dataBinaryFirst.length);
        System.arraycopy(dataBinarySecond, 0, result, dataBinaryFirst.length, dataBinarySecond.length);
        return result;
    }
}
