package com.customanagerapi.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.customanagerapi.domain.entity.Associado;
import com.customanagerapi.domain.entity.Empresa;
import com.customanagerapi.domain.entity.Movimentacao;
import com.customanagerapi.domain.entity.MovimentacaoProduto;
import com.customanagerapi.domain.entity.Produto;
import com.customanagerapi.enums.TipoMovimentacaoEnum;
import com.customanagerapi.repository.AssociadoRepository;
import com.customanagerapi.repository.EmpresaRepository;
import com.customanagerapi.repository.MovimentacaoRepository;
import com.customanagerapi.repository.ProdutoRepository;


@Service
public class MovimentacaoService {
	
	@Autowired
	private MovimentacaoProdutoService movimentacaoProdService;
	
	@Autowired
	private MovimentacaoRepository movimentacaoRepository;
			
	@Autowired
	@Lazy
	private EmpresaRepository empresaRepository;
	
	@Autowired
	@Lazy
	private AssociadoRepository associadoRepository;
	
	@Autowired
	@Lazy
	private ProdutoRepository produtoRepository;
	

	@Transactional
	public Boolean salvar(Movimentacao movimentacao) throws Exception  {	
		
		try {
			
			Empresa emp = empresaRepository.getById(movimentacao.getIdEmpresa());
			movimentacao.setEmpresa(emp);
			
			Associado assoc = associadoRepository.getById(movimentacao.getIdAssociado());
			movimentacao.setAssociado(assoc);
			
			List<MovimentacaoProduto> mp = movimentacao.getMovimentacaoProdutos();				
				
			calcularValorTotalMovimentacao(movimentacao, mp);	
			

			if(!mp.isEmpty() || !(mp == null)) {
				validarStatusProdutos(mp);
			}		
						
			movimentacaoRepository.save(movimentacao);	

			for(MovimentacaoProduto m1 : mp) {
				m1.setMovimentacao(movimentacao);
			}
			
			movimentacaoProdService.salvar(mp);
			
			return true;
			
		}
		
		catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		
		
	}
	
	public Boolean validarStatusProdutos(List<MovimentacaoProduto> mp) throws Exception {
				
		for(MovimentacaoProduto m1 : mp) {	
			
			Produto p = produtoRepository.getById(m1.getProduto().getId());		
			
			if(!p.getAtivo()) {				
				throw new Exception(
						"Um dos produtos escolhidos está inativo. "
						+ "Verifique e tente novamente");
			}			
		}
		
		return true;
	}
	
	
	public Boolean calcularValorTotalMovimentacao(Movimentacao m, List<MovimentacaoProduto> mp) throws Exception {
		
		try {
			
			Double valorTotalMovimentacao = 0.0;
			
			for (MovimentacaoProduto m1 : mp) {
				Double valorTotalProdutoMov = m1.getValorUnitario() * m1.getQuantidade();
				
				valorTotalMovimentacao = valorTotalMovimentacao + valorTotalProdutoMov;
			}
			
			m.setValorTotal(valorTotalMovimentacao);				

			return true;			
		}	
		
		catch (Exception e) {
			throw new Exception("Ocorreu um erro ao calcular valor total da movimentação");
		}
	}
	
	@Transactional
	public Page<Movimentacao> getMovimentacoesByEmpresaId(
			Long empresaId,
			String orderBy, 
			Boolean orderAsc,
			Integer pageNumber, 
			Integer pageSize) {
		
		Empresa emp = empresaRepository.getById(empresaId);
		
		
		Sort sort = orderAsc ? Sort.by(orderBy).ascending() : Sort.by(orderBy).descending();		
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
		
	    Page<Movimentacao> movimentacao = movimentacaoRepository.findByEmpresa(emp, pageable);
	    
		return movimentacao;
	}
	

	@Transactional
	public Optional<Movimentacao> getMovimentacaoById(long id) {						
		Optional<Movimentacao> mov = movimentacaoRepository.findById(id);
		
		if(mov.isPresent()) {			
			Empresa prodEmp = mov.get().getEmpresa();			
			mov.get().setIdEmpresa(prodEmp.getId());
		}		
		
		return mov;
		
	}
	
	public Map<String, String> getMovimentacoesByTipo(Long empresaId) {
		
		Empresa emp = empresaRepository.getById(empresaId);
		
		Long quantVenda = movimentacaoRepository
				.countByEmpresaAndTipoMovimentacao(emp, TipoMovimentacaoEnum.VENDA);
		
		Long quantCompra = movimentacaoRepository
				.countByEmpresaAndTipoMovimentacao(emp, TipoMovimentacaoEnum.COMPRA);
		
		
		
	    HashMap<String, String> map = new HashMap<>();
	    map.put("qt_venda", quantVenda.toString());
	    map.put("qt_compra", quantCompra.toString());
	    
	    return map;
	}
	
	
	@Transactional
	public Page<Movimentacao> search(
			Long empresaId,
			String chave,
			String busca,
			String orderBy, 
			Boolean orderAsc,
			Integer pageNumber, 
			Integer pageSize) throws Exception {
		
		Empresa emp = empresaRepository.getById(empresaId);
		
		Sort sort = orderAsc ? Sort.by(orderBy).ascending() : Sort.by(orderBy).descending();		
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
		
		Page<Movimentacao> prd = null;
		
		switch(chave) {
		
		case "id": 
			long id = Long.parseLong(busca);
			prd = movimentacaoRepository.findByEmpresaAndId(
					emp, 
					id, 
					pageable);
			break;
		
		case "tipoMovimentacao":			
			TipoMovimentacaoEnum tipo = busca.equals("VENDA") ? TipoMovimentacaoEnum.VENDA : TipoMovimentacaoEnum.COMPRA;
			prd = movimentacaoRepository.
			findByEmpresaAndTipoMovimentacao(
							emp, 
							tipo, 
							pageable); 	
			break;
			
		case "associado":			
			prd = movimentacaoRepository.
					findByEmpresaAndAssociado_NomeContainingIgnoreCase(
							emp, 
							busca, 
							pageable);
			break;	
			
		case "descricao":			
			prd = movimentacaoRepository.
					findByEmpresaAndDescricaoContainingIgnoreCase(
							emp, 
							busca, 
							pageable);
			break;	
			
		case "valorTotal":
			Double valor = Double.valueOf(busca);
			prd = movimentacaoRepository.
					findByEmpresaAndValorTotal(
					emp, 
					valor, 
					pageable);
			break;
			
		default: 
			throw new Exception("Chave adicionada inválida.");
			
		}
		
		return prd;
	}
	
	
	@Transactional
	public void delete(Long id) { 
		movimentacaoRepository.deleteById(id);
	}
	
	

}
