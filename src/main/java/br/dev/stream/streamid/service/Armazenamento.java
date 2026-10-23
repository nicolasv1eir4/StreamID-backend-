package br.dev.stream.streamid.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class Armazenamento {

    private final Path raiz;

    public Armazenamento(@Value("${app.upload-dir:uploads}") String pasta) throws IOException {
        this.raiz = Path.of(pasta).toAbsolutePath().normalize();
        Files.createDirectories(raiz);
    }

    /** Salva o arquivo com nome aleatório e devolve esse nome.
     * @param arquivo
     * @return 
     * @throws java.io.IOException */
    public String salvar(MultipartFile arquivo) throws IOException {
        String original = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename();
        int ponto = original.lastIndexOf('.');
        String extensao = ponto >= 0
                ? original.substring(ponto).toLowerCase().replaceAll("[^.a-z0-9]", "")
                : "";
        String nome = UUID.randomUUID() + extensao;

        try (InputStream entrada = arquivo.getInputStream()) {
            Files.copy(entrada, raiz.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
        }
        return nome;
    }

    public Optional<Resource> carregar(String nome) {
        Path caminho = raiz.resolve(nome).normalize();
        if (!caminho.startsWith(raiz) || !Files.isRegularFile(caminho)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(caminho));
    }

    public void excluir(String nome) {
        try {
            Path caminho = raiz.resolve(nome).normalize();
            if (caminho.startsWith(raiz)) Files.deleteIfExists(caminho);
        } catch (IOException ignorado) {
            // arquivo órfão não impede a operação; pode ser limpo depois
        }
    }
}