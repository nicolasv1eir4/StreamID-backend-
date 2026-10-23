package br.dev.stream.streamid.repository;

import br.dev.stream.streamid.model.Midia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MidiaRepository extends JpaRepository<Midia, Long> {
    List<Midia> findByUsuarioIdOrderByDataUploadDesc(Long usuarioId);
    Optional<Midia> findByIdAndUsuarioId(Long id, Long usuarioId);
}
