package space.obminyashka.items_exchange.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import space.obminyashka.items_exchange.dao.ImageRepository;
import space.obminyashka.items_exchange.dto.ImageDto;
import space.obminyashka.items_exchange.exception.UnsupportedMediaTypeException;
import space.obminyashka.items_exchange.model.Advertisement;
import space.obminyashka.items_exchange.model.Image;
import space.obminyashka.items_exchange.service.basic.BasicImageCreator;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ImageServiceTest extends BasicImageCreator{
    @MockBean
    private ImageRepository imageRepository;
    @Autowired
    private ImageService imageService;
    private Image jpeg;
    private MockMultipartFile testJpg;
    private MockMultipartFile testPng;
    private MockMultipartFile testTxt;

    @Captor
    private ArgumentCaptor<List<Image>> imageListCaptor;
    @Captor
    private ArgumentCaptor<Image> imageCaptor;

    @BeforeEach
    void setUp() throws IOException {
        jpeg = new Image(1, "test jpeg".getBytes(), null);
        testJpg = getImageBytes(MediaType.IMAGE_JPEG);
        testPng = getImageBytes(MediaType.IMAGE_PNG);
        testTxt = new MockMultipartFile("image", "text.txt", MediaType.TEXT_PLAIN_VALUE, "plain text".getBytes());
    }

    @Test
    void getImagesResourceByAdvertisementId_shouldReturnAllImagesLinkedToAdvertisement_whenAdvertisementExistsAndContainsImages() {
        when(imageRepository.findByAdvertisementId(any())).thenReturn(List.of(jpeg));

        List<byte[]> result = imageService.getImagesResourceByAdvertisementId(1L);
        assertEquals(jpeg.getResource(), result.get(0), "Images' resources should be equal");
        verify(imageRepository).findByAdvertisementId(anyLong());
    }

    @Test
    void getByAdvertisementId_shouldReturnPopulatedImageDto_whenAdvertisementExistsAndContainsImage() {
        when(imageRepository.findByAdvertisementId(any())).thenReturn(List.of(jpeg));

        ImageDto imageDto = imageService.getByAdvertisementId(1L).get(0);
        assertAll("Checking objects' data equal",
                () -> assertEquals(jpeg.getId(), imageDto.getId()),
                () -> assertArrayEquals(jpeg.getResource(), imageDto.getResource()));
        verify(imageRepository).findByAdvertisementId(anyLong());
    }

    @Test
    void compressImages_shouldCompressImage_WhenValidImageTypes() throws IOException, UnsupportedMediaTypeException {
        List<byte[]> compressed = imageService.compress(List.of(testJpg, testPng));
        assertNotEquals(compressed.get(0).length, testJpg.getBytes().length);
        assertNotEquals(compressed.get(1).length, testPng.getBytes().length);
    }

    @Test
    void compressImage_shouldCompressImage_WhenValidImageType() throws IOException {
        byte[] compressImage = imageService.compress(testJpg);
        assertTrue(testJpg.getBytes().length > compressImage.length);
    }

    @Test
    void compressImage_shouldThrowException_WhenInvalidImageType(){
        assertThrows(UndeclaredThrowableException.class, () -> imageService.compress(testTxt));
    }

    @Test
    void saveToAdvertisement_shouldSaveImageBytes_whenAdvertisementExists() throws IOException {
        List<byte[]> testImages = List.of(testJpg.getBytes(), testPng.getBytes());

        imageService.saveToAdvertisement(new Advertisement(), testImages);
        verify(imageRepository).saveAll(imageListCaptor.capture());
        assertTrue(imageListCaptor.getValue().stream()
                .map(Image::getResource)
                .allMatch(testImages::contains));
    }

    @Test
    void saveToAdvertisement_shouldSaveOneImageBytes_whenAdvertisementExists() throws IOException {
        imageService.saveToAdvertisement(new Advertisement(), testJpg.getBytes());
        verify(imageRepository).save(imageCaptor.capture());
        assertArrayEquals(imageCaptor.getValue().getResource(), testJpg.getBytes());
    }

    @Test
    void removeById_shouldRemoveAllImagesWithReceivedId() {
        List<Long> testImagesId = List.of(1L, 2L, 3L);

        imageService.removeById(testImagesId);
        verify(imageRepository).deleteAllByIdIn(testImagesId);
        verifyNoMoreInteractions(imageRepository);
    }

    @Test
    void removeById_shouldRemoveOneImageWithReceivedId() {
        long imageId = 1L;
        imageService.removeById(imageId);
        verify(imageRepository).deleteById(imageId);
        verifyNoMoreInteractions(imageRepository);
    }

    @Test
    void scale_shouldReturnScaledImage() throws IOException {
        byte[] bytes = testPng.getBytes();
        byte[] result = imageService.scale(bytes);
        assertTrue(bytes.length >= result.length, "Images' resources should have a smaller size");
    }

    @Test
    void scale_shouldReturnScaledJpegImage() throws IOException {
        byte[] bytes = testJpg.getBytes();
        byte[] result = imageService.scale(bytes);
        assertTrue(bytes.length >= result.length, "Images' resources should have a smaller size");
    }
}