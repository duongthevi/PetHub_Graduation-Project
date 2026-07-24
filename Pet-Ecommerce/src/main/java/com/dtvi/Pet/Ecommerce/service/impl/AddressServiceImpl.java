package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.AddressDto;
import com.dtvi.Pet.Ecommerce.dto.Response;
import com.dtvi.Pet.Ecommerce.entity.Address;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.repository.AddressRepo;
import com.dtvi.Pet.Ecommerce.service.interf.AddressService;
import com.dtvi.Pet.Ecommerce.service.interf.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepo addressRepo;
    private final UserService userService;


    @Override
    public Response saveAndUpdateAddress(AddressDto addressDto) {
        User user = userService.getLoginUser();
        Address address = user.getAddress();
        boolean creating = address == null;

        if (creating){
            address = new Address();
            address.setUser(user);
        }
        if (addressDto.getStreet() != null) address.setStreet(addressDto.getStreet());
        if (addressDto.getWard() != null) address.setWard(addressDto.getWard());
        if (addressDto.getCity() != null) address.setCity(addressDto.getCity());
        if (addressDto.getDistrict() != null) address.setDistrict(addressDto.getDistrict());
        if (addressDto.getCountry() != null) address.setCountry(addressDto.getCountry());
        if (addressDto.getRecipientName() != null) address.setRecipientName(addressDto.getRecipientName());
        if (addressDto.getRecipientPhone() != null) address.setRecipientPhone(addressDto.getRecipientPhone());

        addressRepo.save(address);

        String message = creating ? "Address successfully created" : "Address successfully updated";
        return Response.builder()
                .status(200)
                .message(message)
                .build();
    }
}
