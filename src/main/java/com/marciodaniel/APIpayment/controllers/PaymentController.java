package com.marciodaniel.APIpayment.controllers;


import com.marciodaniel.APIpayment.controllers.exceptions.BindingResultException;
import com.marciodaniel.APIpayment.controllers.exceptions.ObjectNotFoundException;
import com.marciodaniel.APIpayment.domain.Buyer;
import com.marciodaniel.APIpayment.domain.Card;
import com.marciodaniel.APIpayment.domain.Client;
import com.marciodaniel.APIpayment.domain.Payment;
import com.marciodaniel.APIpayment.dtos.BuyerDto;
import com.marciodaniel.APIpayment.dtos.CardDto;
import com.marciodaniel.APIpayment.dtos.ClientDto;
import com.marciodaniel.APIpayment.dtos.PaymentDto;
import com.marciodaniel.APIpayment.dtos.PaymentInfoDto;
import com.marciodaniel.APIpayment.enums.PaymentTypeEnum;
import com.marciodaniel.APIpayment.factorys.PaymentFactory;
import com.marciodaniel.APIpayment.services.impl.ClientServiceImpl;
import com.marciodaniel.APIpayment.services.impl.PaymentServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin("*")
public class PaymentController {

    private final Logger logger = LogManager.getLogger(PaymentController.class);

    private final ClientServiceImpl clientService;

    private final PaymentServiceImpl paymentService;

    @Autowired
    public PaymentController(ClientServiceImpl clientService, PaymentServiceImpl paymentService) {
        this.clientService = clientService;
        this.paymentService = paymentService;
    }

    @PostMapping("/conclude")
    public ResponseEntity<PaymentInfoDto> conclude(@Valid @RequestBody PaymentInfoDto paymentInfoDto, BindingResult result) {

        logger.info("Valid has Errors");
        if (result.hasErrors()) {
            logger.error("Errors on valid Payment Data", result.getAllErrors());

            throw  new BindingResultException("Valid errors message", result);
        }

        logger.info("Convert DTO to Payment");
        Payment payment = paymentInfoDto.paymentFor();

        logger.info("Searching for client");
        Optional<Client> client = this.clientService.findById(payment.getClient().getId());

        client.orElseThrow(() -> new ObjectNotFoundException("Client not found, id: " + payment.getClient().getId() + ", Type: " + Client.class.getName()));

        logger.info("insert payment");
        this.paymentService.save(payment);

        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(PaymentInfoDto.paymentInfoDtoFor(payment));
    }

    @GetMapping("/status")
    public ResponseEntity<PaymentInfoDto> checkPaymentStatus(@RequestParam(value = "idPayment") Long idPayment) {
        logger.info("Searching payment for check status");
        Optional<Payment> payment = this.paymentService.findById(idPayment);

        payment.orElseThrow(() -> new ObjectNotFoundException("Payment not found, id: " + idPayment + ", Type: " + Payment.class.getName()));

        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(payment.map(PaymentInfoDto::paymentInfoDtoFor).orElse(null));
    }

    @GetMapping("/all")
    public ResponseEntity<List<PaymentInfoDto>> listAll() {
        logger.info("List All Payments");
        List<Payment> payments = this.paymentService.findAll();

        List<PaymentInfoDto> paymentInfoDtos = new ArrayList<>();

        payments.forEach(payment -> paymentInfoDtos.add(PaymentInfoDto.paymentInfoDtoFor(payment)));

        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(paymentInfoDtos);
    }
}