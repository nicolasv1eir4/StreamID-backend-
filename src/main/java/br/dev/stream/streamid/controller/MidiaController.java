package br.dev.stream.streamid.controller;

import br.dev.stream.streamid.model.Midia;
import br.dev.stream.streamid.model.Usuario;
import br.dev.stream.streamid.repository.MidiaRepository;
import br.dev.stream.streamid.repository.UsuarioRepository;
import br.dev.stream.streamid.service.ArmazenamentoService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/midias")
public class MidiaController {

    public record MidiaResponse(Long id, String titulo, String nomeOriginal, String tipoMime,
                                long tamanho, LocalDateTime dataUpload, String url) {}

    public record AtualizarRequest(String titulo) {}

    private final MidiaRepository midiaRepo;
    private final UsuarioRepository usuarioRepo;
    private final ArmazenamentoService armazenamento;

    public MidiaController(MidiaRepository midiaRepo, UsuarioRepository usuarioRepo,
                           ArmazenamentoService armazenamento) {
        this.midiaRepo = midiaRepo;
        this.usuarioRepo = usuarioRepo;
        this.armazenamento = armazenamento;
    }

    private MidiaResponse paraResposta(Midia m, Long usuarioId) {
        return new MidiaResponse(m.getId(), m.getTitulo(), m.getNomeOriginal(), m.getTipoMime(),
                m.getTamanho(), m.getDataUpload(),
                "api/midias/" + m.getId() + "/arquivo?usuarioId=" + usuarioId);
    }

    private static ResponseEntity<Map<String, String>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }

    // CREATE: envia uma mídia
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("arquivo") MultipartFile arquivo,
                                    @RequestParam("usuarioId") Long usuarioId,
                                    @RequestParam(value = "titulo", required = false) String titulo) {
        Usuario usuario = usuarioRepo.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return erro(HttpStatus.UNAUTHORIZED, "Usuário não encontrado. Faça login novamente.");
        }
        if (arquivo.isEmpty()) {
            return erro(HttpStatus.BAD_REQUEST, "O arquivo está vazio.");
        }
        String tipo = arquivo.getContentType();
        if (tipo == null || !(tipo.startsWith("image/") || tipo.startsWith("video/") || tipo.startsWith("audio/"))) {
            return erro(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie apenas imagem, vídeo ou áudio.");
        }

        String nomeArquivo;
        try {
            nomeArquivo = armazenamento.salvar(arquivo);
        } catch (IOException e) {
            return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao gravar o arquivo no servidor.");
        }

        String original = arquivo.getOriginalFilename() == null ? "arquivo" : arquivo.getOriginalFilename();
        Midia m = new Midia();
        m.setTitulo(titulo == null || titulo.isBlank() ? original : titulo.trim());
        m.setNomeOriginal(original);
        m.setNomeArquivo(nomeArquivo);
        m.setTipoMime(tipo);
        m.setTamanho(arquivo.getSize());
        m.setUsuario(usuario);

        try {
            midiaRepo.save(m);
        } catch (RuntimeException e) {
            armazenamento.excluir(nomeArquivo);   // não deixa arquivo sem registro no banco
            throw e;
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(m, usuarioId));
    }

    // READ: lista as mídias do usuário
    @GetMapping
    public List<MidiaResponse> listar(@RequestParam("usuarioId") Long usuarioId) {
        return midiaRepo.findByUsuarioIdOrderByDataUploadDesc(usuarioId).stream()
                .map(m -> paraResposta(m, usuarioId))
                .toList();
    }

    // READ: dados de uma mídia
    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable("id") Long id,
                                    @RequestParam("usuarioId") Long usuarioId) {
        return midiaRepo.findByIdAndUsuarioId(id, usuarioId)
                .<ResponseEntity<?>>map(m -> ResponseEntity.ok(paraResposta(m, usuarioId)))
                .orElseGet(() -> erro(HttpStatus.NOT_FOUND, "Mídia não encontrada."));
    }

    // READ: entrega o arquivo (serve para <img>, <video> e <audio>, com suporte a Range)
    @GetMapping("/{id}/arquivo")
    public ResponseEntity<Resource> arquivo(@PathVariable("id") Long id,
                                            @RequestParam("usuarioId") Long usuarioId) {
        Midia m = midiaRepo.findByIdAndUsuarioId(id, usuarioId).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();

        return armazenamento.carregar(m.getNomeArquivo())
                .map(recurso -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(m.getTipoMime()))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.inline()
                                        .filename(m.getNomeOriginal(), StandardCharsets.UTF_8)
                                        .build().toString())
                        .body(recurso))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // UPDATE: renomeia o título
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable("id") Long id,
                                       @RequestParam("usuarioId") Long usuarioId,
                                       @RequestBody AtualizarRequest req) {
        if (req.titulo() == null || req.titulo().isBlank()) {
            return erro(HttpStatus.BAD_REQUEST, "Informe um título.");
        }
        return midiaRepo.findByIdAndUsuarioId(id, usuarioId)
                .<ResponseEntity<?>>map(m -> {
                    m.setTitulo(req.titulo().trim());
                    midiaRepo.save(m);
                    return ResponseEntity.ok(paraResposta(m, usuarioId));
                })
                .orElseGet(() -> erro(HttpStatus.NOT_FOUND, "Mídia não encontrada."));
    }

    // DELETE: remove do banco e do disco
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable("id") Long id,
                                     @RequestParam("usuarioId") Long usuarioId) {
        Midia m = midiaRepo.findByIdAndUsuarioId(id, usuarioId).orElse(null);
        if (m == null) return erro(HttpStatus.NOT_FOUND, "Mídia não encontrada.");

        midiaRepo.delete(m);
        armazenamento.excluir(m.getNomeArquivo());
        return ResponseEntity.noContent().build();
    }
}