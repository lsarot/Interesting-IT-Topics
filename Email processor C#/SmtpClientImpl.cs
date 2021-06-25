namespace LED.Managers
{
    using System;
    using System.Collections.Generic;
    using System.Net.Mail;
    using System.Text;
    using System.Threading.Tasks;

    public class SmtpClientImpl : ISmtpClient
    {
        private readonly SmtpClient _smtpClient;

        public SmtpClientImpl(SmtpClient client)
        {
            _smtpClient = client;
        }

        public void Dispose()
        {
            _smtpClient.Dispose();
        }

        public Task SendMailAsync(MailMessage message)
        {
            return _smtpClient.SendMailAsync(message);
        }
    }
}
