package com.example.server_android.words;


import jakarta.persistence.*;
import lombok.*;



@Setter
@ToString
@RequiredArgsConstructor
@Getter
@Builder
@AllArgsConstructor
@Entity
@Table(name = "words")
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String englishWord;
    private String romanianWord;
}
