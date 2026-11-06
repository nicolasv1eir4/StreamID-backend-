package br.dev.stream.streamid.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;   // se usar Spring Boot 2, troque por javax.persistence.*
import java.util.ArrayList;
import java.util.List;

@Entity
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;

    @JsonIgnore // evita expor os dados do usuário (senha, etc.) no JSON
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToMany
    @JoinTable(
        name = "playlist_midia",
        joinColumns = @JoinColumn(name = "playlist_id"),
        inverseJoinColumns = @JoinColumn(name = "midia_id")
    )
    private List<Midia> midias = new ArrayList<>();

    public Playlist() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public List<Midia> getMidias() { return midias; }
    public void setMidias(List<Midia> midias) { this.midias = midias; }
  
}
