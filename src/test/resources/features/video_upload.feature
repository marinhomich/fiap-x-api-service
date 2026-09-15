# language: pt
Funcionalidade: Ingestão e Processamento de Vídeos na Plataforma FIAP X

  Como um usuário autenticado na plataforma FIAP X
  Eu quero enviar meus arquivos de vídeo
  Para que eles sejam processados assincronamente e os frames sejam extraídos em um arquivo ZIP

  Cenário: Upload de vídeo válido com sucesso
    Dado que o usuário está autenticado com o e-mail "michel@fiap.com.br"
    Quando o usuário envia um arquivo de vídeo "apresentacao.mp4" com formato válido
    Então o sistema deve aceitar a requisição com status HTTP 202
    E o vídeo deve ser registrado com status "RECEBIDO"
    E uma mensagem de processamento deve ser enviada para a fila do RabbitMQ

  Cenário: Rejeição de arquivo com extensão inválida
    Dado que o usuário está autenticado com o e-mail "michel@fiap.com.br"
    Quando o usuário envia um arquivo inválido "script_malicioso.exe"
    Então o sistema deve rejeitar a requisição informando formato não suportado

  Cenário: Consulta do status de processamento do vídeo
    Dado que o usuário possui um vídeo cadastrado com status "CONCLUIDO"
    Quando o usuário solicita a listagem dos seus vídeos
    Então o sistema deve retornar a lista contendo o vídeo e a URL para download do ZIP
