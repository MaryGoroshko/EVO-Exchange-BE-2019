package space.obminyashka.items_exchange.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import space.obminyashka.items_exchange.api.ApiKey;
import space.obminyashka.items_exchange.dto.AdvertisementDisplayDto;
import space.obminyashka.items_exchange.dto.AdvertisementFilterDto;
import space.obminyashka.items_exchange.dto.AdvertisementModificationDto;
import space.obminyashka.items_exchange.dto.AdvertisementTitleDto;
import space.obminyashka.items_exchange.exception.BadRequestException;
import space.obminyashka.items_exchange.exception.IllegalIdentifierException;
import space.obminyashka.items_exchange.exception.IllegalOperationException;
import space.obminyashka.items_exchange.mapper.transfer.Exist;
import space.obminyashka.items_exchange.mapper.transfer.New;
import space.obminyashka.items_exchange.model.User;
import space.obminyashka.items_exchange.service.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.security.Principal;
import java.util.List;

import static space.obminyashka.items_exchange.util.MessageSourceUtil.getExceptionMessageSourceWithId;
import static space.obminyashka.items_exchange.util.MessageSourceUtil.getMessageSource;

@RestController
@RequestMapping(ApiKey.ADV)
@Api(tags = "Advertisement")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdvertisementController {

    private final AdvertisementService advertisementService;
    private final ImageService imageService;
    private final UserService userService;
    private final SubcategoryService subcategoryService;
    private final LocationService locationService;

    @GetMapping("/thumbnail")
    @ApiOperation(value = "Find requested quantity of the advertisement as thumbnails and return them as a page result")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "BAD REQUEST"),
            @ApiResponse(code = 404, message = "NOT FOUND")})
    public ResponseEntity<List<AdvertisementTitleDto>> findPaginatedAsThumbnails(
            @ApiParam(value = "Results page you want to retrieve (0..N). Default value: 0")
            @RequestParam(value = "page", required = false, defaultValue = "0") @PositiveOrZero int page,
            @ApiParam(value = "Number of records per page. Default value: 12")
            @RequestParam(value = "size", required = false, defaultValue = "12") @PositiveOrZero int size) {
        List<AdvertisementTitleDto> dtoList = advertisementService.findAllThumbnails(PageRequest.of(page, size));
        return dtoList.isEmpty() ?
                new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @GetMapping("/thumbnail/random")
    @ApiOperation(value = "Find 12 random advertisement as thumbnails and return them as a result")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Database is empty")})
    public ResponseEntity<List<AdvertisementTitleDto>> findRandom12Thumbnails() {
        final var dtoList = advertisementService.findRandom12Thumbnails();
        return dtoList.isEmpty() ?
                new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @GetMapping("/total-amount")
    @ApiOperation(value = "Count existed advertisements and return total records amount number")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Database is empty")})
    public ResponseEntity<Long> countAdvertisements() {
        final long totalAmount = advertisementService.count();
        return totalAmount == 0 ?
                new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                new ResponseEntity<>(totalAmount, HttpStatus.OK);
    }

    @GetMapping("/{advertisement_id}")
    @ApiOperation(value = "Find an advertisement by its ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "BAD REQUEST"),
            @ApiResponse(code = 404, message = "NOT FOUND")})
    public ResponseEntity<AdvertisementDisplayDto> getAdvertisement(
            @ApiParam(value = "ID of existed advertisement")
            @PathVariable("advertisement_id") @PositiveOrZero(message = "{invalid.id}") Long id) {
        return ResponseEntity.of(advertisementService.findDtoById(id));
    }

    @GetMapping("/search/{keyword}")
    @ApiOperation(value = "Find advertisements by keyword")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "BAD REQUEST"),
            @ApiResponse(code = 404, message = "NOT FOUND")})
    public ResponseEntity<Page<AdvertisementTitleDto>> getPageOfAdvertisementsByKeyword(
            @PathVariable @NotEmpty String keyword,
            @ApiParam(value = "Results page you want to retrieve (0..N). Default value: 0")
            @RequestParam(value = "page", required = false, defaultValue = "0") @PositiveOrZero int page,
            @ApiParam(value = "Number of records per page. Default value: 12")
            @RequestParam(value = "size", required = false, defaultValue = "12") @PositiveOrZero int size){
        Page<AdvertisementTitleDto> allByKeyword = advertisementService.findByKeyword(keyword, PageRequest.of(page, size, Sort.by("topic")));
        return allByKeyword.isEmpty() ?
                new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                new ResponseEntity<>(allByKeyword, HttpStatus.OK);
    }

    @PostMapping("/filter")
    @ApiOperation(value = "Filter advertisements by multiple params and return up to 10 results.\n" +
            "Fill only needed parameters.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "BAD REQUEST"),
            @ApiResponse(code = 404, message = "NOT FOUND")})
    public ResponseEntity<List<AdvertisementTitleDto>> getFirst10BySearchParameters(@Valid @RequestBody AdvertisementFilterDto filterDto) {
        List<AdvertisementTitleDto> advertisementsByMultipleParams = advertisementService.findFirst10ByFilter(filterDto);
        return advertisementsByMultipleParams.isEmpty() ?
                new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                new ResponseEntity<>(advertisementsByMultipleParams, HttpStatus.OK);
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ApiOperation(value = "Create a new advertisement")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CREATED"),
            @ApiResponse(code = 400, message = "BAD REQUEST"),
            @ApiResponse(code = 403, message = "FORBIDDEN")})
    @ResponseStatus(HttpStatus.CREATED)
    public AdvertisementModificationDto createAdvertisement(
            @Validated(New.class) @Valid @RequestPart AdvertisementModificationDto dto,
            @RequestPart(value = "image") @Size(min = 1, max = 10) List<MultipartFile> images,
            @ApiIgnore Principal principal) throws IllegalIdentifierException {

        validateInternalEntityIds(dto.getSubcategoryId(), dto.getLocationId());
        final var owner = getUser(principal.getName());
        final var compressedImages = images.parallelStream()
                .map(imageService::compress)
                .toList();
        return advertisementService.createAdvertisement(dto, owner, compressedImages);
    }

    @PutMapping
    @ApiOperation(value = "Update an existed advertisement")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "ACCEPTED"),
            @ApiResponse(code = 400, message = "BAD REQUEST"),
            @ApiResponse(code = 404, message = "NOT FOUND"),
            @ApiResponse(code = 403, message = "FORBIDDEN")})
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AdvertisementModificationDto updateAdvertisement(@Validated(Exist.class)
                                                            @Valid @RequestBody AdvertisementModificationDto dto,
                                                            @ApiIgnore Principal principal)
            throws IllegalIdentifierException, IllegalOperationException {

        validateAdvertisementOwner(dto.getId(), getUser(principal.getName()));
        validateInternalEntityIds(dto.getSubcategoryId(), dto.getLocationId());
        return advertisementService.updateAdvertisement(dto);
    }

    @DeleteMapping("/{advertisement_id}")
    @ApiOperation(value = "Delete an existed advertisement")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "BAD REQUEST"),
            @ApiResponse(code = 403, message = "FORBIDDEN")})
    @ResponseStatus(HttpStatus.OK)
    public void deleteAdvertisement(@PathVariable("advertisement_id") @Positive(message = "{invalid.exist.id}") long id,
                                    @ApiIgnore Principal principal)
            throws IllegalOperationException {

        User owner = getUser(principal.getName());
        validateAdvertisementOwner(id, owner);
        advertisementService.remove(id);
    }

    @PostMapping("/default-image/{advertisementId}/{imageId}")
    @ApiOperation(value = "Set a default image to an existed advertisement")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "BAD REQUEST"),
            @ApiResponse(code = 403, message = "FORBIDDEN")})
    @ResponseStatus(HttpStatus.OK)
    public void setDefaultImage(
            @ApiParam(value = "ID of existed advertisement")
            @PathVariable @PositiveOrZero(message = "{invalid.id}") Long advertisementId,
            @ApiParam(value = "ID of existed image")
            @PathVariable @PositiveOrZero(message = "{invalid.id}") Long imageId,
            @ApiIgnore Principal principal) throws BadRequestException {
        User owner = getUser(principal.getName());
        if (!advertisementService.isUserHasAdvertisementAndItHasImageWithId(advertisementId, imageId, owner)) {
            throw new BadRequestException(getMessageSource("exception.advertisement-image.id.not-found"));
        }
        owner.getAdvertisements().parallelStream()
                .filter(advertisement -> advertisement.getId() == advertisementId)
                .findFirst()
                .ifPresent(adv -> advertisementService.setDefaultImage(adv, imageId));
    }

    private void validateAdvertisementOwner(long advertisementId, User owner) throws IllegalOperationException {
        if (!advertisementService.isUserHasAdvertisementWithId(advertisementId, owner)) {
            throw new IllegalOperationException(getMessageSource("user.not-owner"));
        }
    }

    private void validateInternalEntityIds(long subcategoryId, long locationId) throws IllegalIdentifierException {
        var exceptionMessage = "";
        if (!subcategoryService.isSubcategoryExistsById(subcategoryId)) {
            exceptionMessage = getExceptionMessageSourceWithId(subcategoryId, "invalid.subcategory.id") + "\n";
        }
        if (!locationService.existsById(locationId)) {
            exceptionMessage = exceptionMessage.concat(getExceptionMessageSourceWithId(subcategoryId, "invalid.location.id"));
        }
        if (!exceptionMessage.isEmpty()) {
            throw new IllegalIdentifierException(exceptionMessage);
        }
    }

    private User getUser(String userNameOrEmail) {
        return userService.findByUsernameOrEmail(userNameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(getMessageSource("exception.user.not-found")));
    }
}
