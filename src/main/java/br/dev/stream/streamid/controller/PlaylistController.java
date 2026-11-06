package br.dev.stream.streamid.controller;

import br.dev.stream.streamid.model.Midia;
import br.dev.stream.streamid.model.Playlist;
import br.dev.stream.streamid.model.Usuario;
import br.dev.stream.streamid.repository.MidiaRepository;
import br.dev.stream.streamid.repository.PlaylistRepository;
import br.dev.stream.streamid.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistRepository playlistRepository;
    private final UsuarioRepository usuarioRepository;
    private final MidiaRepository midiaRepository;

    public PlaylistController(PlaylistRepository playlistRepository,
                              UsuarioRepository usuarioRepository,
                              MidiaRepository midiaRepository) {
        this.playlistRepository = playlistRepository;
        this.usuarioRepository = usuarioRepository;
        this.midiaRepository = midiaRepository;
    }

    // Listar playlists de um usuário
    @GetMapping
    public List<Playlist> listar(@RequestParam Long usuarioId) {
        return playlistRepository.findByUsuarioId(usuarioId);
    }

    // Buscar uma playlist
    @GetMapping("/{id}")
    public ResponseEntity<Playlist> buscar(@PathVariable Long id) {
        return playlistRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Criar playlist
    @PostMapping
    public ResponseEntity<Playlist> criar(@RequestParam Long usuarioId,
                                          @RequestBody Playlist playlist) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return ResponseEntity.badRequest().build();
        }
        playlist.setId(null);
        playlist.setUsuario(usuario);
        return ResponseEntity.ok(playlistRepository.save(playlist));
    }

    // Atualizar nome/descrição
    @PutMapping("/{id}")
    public ResponseEntity<Playlist> atualizar(@PathVariable Long id,
                                              @RequestBody Playlist dados) {
        return playlistRepository.findById(id).map(p -> {
            p.setNome(dados.getNome());
            p.setDescricao(dados.getDescricao());
            return ResponseEntity.ok(playlistRepository.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Excluir playlist
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!playlistRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        playlistRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Adicionar mídia à playlist
    @PostMapping("/{id}/midias/{midiaId}")
    public ResponseEntity<Playlist> adicionarMidia(@PathVariable Long id,
                                                   @PathVariable Long midiaId) {
        Playlist playlist = playlistRepository.findById(id).orElse(null);
        Midia midia = midiaRepository.findById(midiaId).orElse(null);
        if (playlist == null || midia == null) {
            return ResponseEntity.notFound().build();
        }
        if (!playlist.getMidias().contains(midia)) {
            playlist.getMidias().add(midia);
        }
        return ResponseEntity.ok(playlistRepository.save(playlist));
    }

    // Remover mídia da playlist
    @DeleteMapping("/{id}/midias/{midiaId}")
    public ResponseEntity<Playlist> removerMidia(@PathVariable Long id,
                                                 @PathVariable Long midiaId) {
        return playlistRepository.findById(id).map(p -> {
            p.getMidias().removeIf(m -> m.getId().equals(midiaId));
            return ResponseEntity.ok(playlistRepository.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }
}