package br.dev.stream.streamid.controller;

import br.dev.stream.streamid.model.Usuario;
import br.dev.stream.streamid.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
@RequestMapping("/api")
public class AuthController {

    public record CadastroRequest(String nome, String email, String senha) {}
    public record LoginRequest(String email, String senha) {}

    private final UsuarioRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UsuarioRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@RequestBody CadastroRequest req) {
        if (req.nome() == null || req.nome().isBlank()
                || req.email() == null || req.email().isBlank()
                || req.senha() == null || req.senha().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", "Preencha todos os campos (senha com no mínimo 6 caracteres)."));
        }
        if (repo.existsByEmail(req.email())) {
            return ResponseEntity.status(409).body(Map.of("erro", "Este email já está cadastrado."));
        }

        Usuario u = new Usuario();
        u.setNome(req.nome());
        u.setEmail(req.email());
        u.setSenhaHash(encoder.encode(req.senha()));
        repo.save(u);

        return ResponseEntity.status(201).body(Map.of("id", u.getId(), "nome", u.getNome()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        return repo.findByEmail(req.email())
                .filter(u -> encoder.matches(req.senha(), u.getSenhaHash()))
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("id", u.getId(), "nome", u.getNome())))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("erro", "Email ou senha inválidos.")));
    }
}
