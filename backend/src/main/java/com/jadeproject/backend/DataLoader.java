package com.jadeproject.backend;

import com.jadeproject.backend.model.User;
import com.jadeproject.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*Configuration explica ao Spring que esta classe não é um objeto comum, mas funciona como um "manual de instruções"
* Quando o sistema inicia, ele procura todas as classes com @Configuration primeiro.
* Ele lê essas classes para saber como montar os outros pedaços do sistema (como conexões de banco, segurança ou beans personalizados).*/
@Configuration
public class DataLoader {

    /*Esta anotação vai em cima de um método (dentro de uma classe @Configuration)
    * Ela diz que o retorno deste método deve ser gerenciado pelo Spring e ficar disponível para quem precisar
    * O Spring executa o método uma vez, pega o objeto que retornou (CommandLineRunner) e guarda no "depósito" de dependências dele*/
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {

            //Verifica se JÁ EXISTE alguém com esse username
            if (userRepository.findByUsername("admin2_teste").isEmpty()) {

                User user = new User();
                user.setUsername("admin2_teste");
                user.setEmail("admin2@jade.com");
                user.setPasswordHash("123456");

                userRepository.save(user);
                System.out.println("SUCESSO: usuário criado. VERIFICAR NO SGBD!!!!!!");

            } else {
                System.out.println("Usuário já existe, pulando criação.");
            }
        };
    }
}