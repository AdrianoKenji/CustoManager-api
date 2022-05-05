package com.customanagerapi.rest.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.customanagerapi.domain.entity.Associado;
import com.customanagerapi.service.AssociadoService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/associados")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "https://customanager.netlify.app"})
public class AssociadoController {
	
	private final AssociadoService associadoService; 
	
	
	@PostMapping("/register")
	@ApiOperation("Cadastro de associados")
	@ResponseStatus(HttpStatus.CREATED)
	public Associado insertAssociado(@RequestBody @Valid Associado associado) throws Exception {		
		return associadoService.salvar(associado);
	}
	

}
