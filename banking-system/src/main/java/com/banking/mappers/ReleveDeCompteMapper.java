package com.banking.mappers;

import com.banking.dto.response.ReleveDeCompteResponse;
import com.banking.entities.Operation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReleveDeCompteMapper {

    @Mapping(target = "dateOperation",
            expression = "java(op.getDateOperation() != null ? op.getDateOperation().toLocalDate() : null)")
    @Mapping(target = "type",
            expression = "java(op.getType() != null ? op.getType().name() : null)")
    @Mapping(target = "montant",      source = "montant")
    @Mapping(target = "description",  source = "description")
    ReleveDeCompteResponse.OperationDto toDto(Operation op);

    List<ReleveDeCompteResponse.OperationDto> toDto(List<Operation> ops);
}
